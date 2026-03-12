Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"
$script:DotEnvLoaded = $false

function Import-ProjectDotEnv {
    param([string]$Path = (Join-Path $PSScriptRoot ".env"))

    if ($script:DotEnvLoaded) {
        return
    }

    if (Test-Path $Path) {
        foreach ($line in Get-Content $Path) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith("#")) {
                continue
            }

            $separatorIndex = $trimmed.IndexOf("=")
            if ($separatorIndex -lt 1) {
                continue
            }

            $name = $trimmed.Substring(0, $separatorIndex).Trim()
            $value = $trimmed.Substring($separatorIndex + 1).Trim()
            if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
                $value = $value.Substring(1, $value.Length - 2)
            }

            [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        }
    }

    $script:DotEnvLoaded = $true
}

function Get-ConfigValue {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Default
    )

    $value = [System.Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }
    return $value
}

function Get-ConfigIntValue {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][int]$Default
    )

    $value = [System.Environment]::GetEnvironmentVariable($Name, "Process")
    if ([string]::IsNullOrWhiteSpace($value)) {
        return $Default
    }
    return [int]$value
}

Import-ProjectDotEnv

$script:ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$script:BackendRoot = Join-Path $script:ProjectRoot "lifeos-backend"
$script:RuntimeRoot = Join-Path $script:ProjectRoot ".runtime"
$script:LogRoot = Join-Path $script:RuntimeRoot "logs"
$script:PidRoot = Join-Path $script:RuntimeRoot "pids"
$script:MySqlHost = Get-ConfigValue -Name "MYSQL_HOST" -Default "127.0.0.1"
$script:MySqlPort = Get-ConfigIntValue -Name "MYSQL_PORT" -Default 3306
$script:RedisHost = Get-ConfigValue -Name "REDIS_HOST" -Default "127.0.0.1"
$script:RedisPort = Get-ConfigIntValue -Name "REDIS_PORT" -Default 6379
$script:NacosHost = Get-ConfigValue -Name "NACOS_HOST" -Default "127.0.0.1"
$script:NacosPort = Get-ConfigIntValue -Name "NACOS_PORT" -Default 8848
$script:NacosGrpcPort = Get-ConfigIntValue -Name "NACOS_GRPC_PORT" -Default 9848

$script:Services = @(
    [pscustomobject]@{ Name = "lifeos-user-service"; Module = "lifeos-user-service"; Port = (Get-ConfigIntValue -Name "LIFEOS_USER_SERVICE_PORT" -Default 8081); RequiresNacos = $true },
    [pscustomobject]@{ Name = "lifeos-task-service"; Module = "lifeos-task-service"; Port = (Get-ConfigIntValue -Name "LIFEOS_TASK_SERVICE_PORT" -Default 8082); RequiresNacos = $true },
    [pscustomobject]@{ Name = "lifeos-note-service"; Module = "lifeos-note-service"; Port = (Get-ConfigIntValue -Name "LIFEOS_NOTE_SERVICE_PORT" -Default 8083); RequiresNacos = $true },
    [pscustomobject]@{ Name = "lifeos-ai-service"; Module = "lifeos-ai-service"; Port = (Get-ConfigIntValue -Name "LIFEOS_AI_SERVICE_PORT" -Default 8084); RequiresNacos = $true },
    [pscustomobject]@{ Name = "lifeos-behavior-service"; Module = "lifeos-behavior-service"; Port = (Get-ConfigIntValue -Name "LIFEOS_BEHAVIOR_SERVICE_PORT" -Default 8085); RequiresNacos = $true },
    [pscustomobject]@{ Name = "lifeos-gateway"; Module = "lifeos-gateway"; Port = (Get-ConfigIntValue -Name "LIFEOS_GATEWAY_PORT" -Default 8080); RequiresNacos = $true }
)

function Ensure-RuntimeLayout {
    foreach ($path in @($script:RuntimeRoot, $script:LogRoot, $script:PidRoot)) {
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
        }
    }
}

function Get-ServiceDefinitions {
    return $script:Services
}

function Get-ServiceByName {
    param([Parameter(Mandatory = $true)][string]$Name)

    return $script:Services | Where-Object { $_.Name -eq $Name }
}

function Get-LogPath {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    return Join-Path $script:LogRoot ($Service.Name + ".log")
}

function Get-PidPath {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    return Join-Path $script:PidRoot ($Service.Name + ".pid")
}

function Get-ModulePomPath {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    return Join-Path $script:BackendRoot (Join-Path $Service.Module "pom.xml")
}

function Test-TcpPort {
    param(
        [Parameter(Mandatory = $true)][string]$Address,
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutMs = 1000
    )

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($Address, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs, $false)) {
            return $false
        }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Dispose()
    }
}

function Wait-ForPort {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -Address "127.0.0.1" -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ForPortClosed {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-TcpPort -Address "127.0.0.1" -Port $Port)) {
            return $true
        }
        Start-Sleep -Seconds 1
    }
    return $false
}

function Test-NacosRegistration {
    param([Parameter(Mandatory = $true)][string]$ServiceName)

    $serviceNames = @(
        $ServiceName,
        "DEFAULT_GROUP@@$ServiceName"
    )

    foreach ($candidate in $serviceNames) {
        try {
            $encodedName = [System.Uri]::EscapeDataString($candidate)
            $uri = "http://$($script:NacosHost):$($script:NacosPort)/nacos/v1/ns/instance/list?serviceName=$encodedName&groupName=DEFAULT_GROUP"
            $response = Invoke-WebRequest -Uri $uri -UseBasicParsing -TimeoutSec 5
            $payload = $response.Content | ConvertFrom-Json
            $healthyHosts = @($payload.hosts | Where-Object { $_.healthy -and $_.enabled })
            if ($healthyHosts.Count -gt 0) {
                return $true
            }
        } catch {
            continue
        }
    }

    return $false
}

function Wait-ForNacosRegistration {
    param(
        [Parameter(Mandatory = $true)][string]$ServiceName,
        [int]$TimeoutSeconds = 60
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-NacosRegistration -ServiceName $ServiceName) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Get-RecordedPid {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    $pidPath = Get-PidPath -Service $Service
    if (-not (Test-Path $pidPath)) {
        return $null
    }

    $value = (Get-Content -Raw $pidPath).Trim()
    if (-not $value) {
        Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
        return $null
    }

    return [int]$value
}

function Save-RecordedPid {
    param(
        [Parameter(Mandatory = $true)][pscustomobject]$Service,
        [Parameter(Mandatory = $true)][int]$ProcessId
    )

    $ProcessId | Set-Content -Path (Get-PidPath -Service $Service) -Encoding ASCII
}

function Clear-RecordedPid {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    Remove-Item (Get-PidPath -Service $Service) -Force -ErrorAction SilentlyContinue
}

function Test-RecordedProcessAlive {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    $recordedPid = Get-RecordedPid -Service $Service
    if (-not $recordedPid) {
        return $false
    }

    $process = Get-Process -Id $recordedPid -ErrorAction SilentlyContinue
    if (-not $process) {
        Clear-RecordedPid -Service $Service
        return $false
    }

    return $true
}

function Get-PortOwnerPid {
    param([Parameter(Mandatory = $true)][int]$Port)

    $matches = netstat -ano | Select-String ":$Port\s+.*LISTENING\s+(\d+)$"
    foreach ($match in $matches) {
        if ($match.Matches.Count -gt 0) {
            return [int]$match.Matches[0].Groups[1].Value
        }
    }
    return $null
}

function Get-LogTail {
    param(
        [Parameter(Mandatory = $true)][pscustomobject]$Service,
        [int]$Lines = 40
    )

    $logPath = Get-LogPath -Service $Service
    if (-not (Test-Path $logPath)) {
        return @("<log file not found>")
    }

    return Get-Content -Path $logPath -Tail $Lines
}

function Assert-Dependencies {
    $checks = @(
        @{ Name = "MySQL"; Host = $script:MySqlHost; Port = $script:MySqlPort },
        @{ Name = "Redis"; Host = $script:RedisHost; Port = $script:RedisPort },
        @{ Name = "Nacos HTTP"; Host = $script:NacosHost; Port = $script:NacosPort },
        @{ Name = "Nacos gRPC"; Host = $script:NacosHost; Port = $script:NacosGrpcPort }
    )

    foreach ($check in $checks) {
        if (-not (Test-TcpPort -Address $check.Host -Port $check.Port)) {
            throw "$($check.Name) is not reachable at $($check.Host):$($check.Port)."
        }
    }
}

function Build-SharedModules {
    $backendPom = Join-Path $script:BackendRoot "pom.xml"
    $command = "mvn.cmd -f `"$backendPom`" -pl lifeos-common,lifeos-api -am install -DskipTests"
    Write-Host "[build] Ensuring shared modules are installed (lifeos-common, lifeos-api)..."
    $result = & cmd.exe /c $command
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to build shared modules. Check Maven output above."
    }
}

function Start-BackendService {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    $portOwner = Get-PortOwnerPid -Port $Service.Port
    if ($portOwner) {
        Write-Host "[skip] $($Service.Name) is already listening on port $($Service.Port) (PID $portOwner)."
        return [pscustomobject]@{ Name = $Service.Name; Status = "already-running"; Port = $Service.Port }
    }

    $logPath = Get-LogPath -Service $Service
    Set-Content -Path $logPath -Value "" -Encoding ASCII

    $modulePomPath = Get-ModulePomPath -Service $Service
    $command = "& 'mvn.cmd' -f '$modulePomPath' spring-boot:run *> '$logPath'"
    $process = Start-Process -FilePath "powershell.exe" `
        -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $command) `
        -WorkingDirectory $script:BackendRoot `
        -WindowStyle Hidden `
        -PassThru

    Save-RecordedPid -Service $Service -ProcessId $process.Id
    Write-Host "[start] $($Service.Name) -> port $($Service.Port) (PID $($process.Id))"

    if (-not (Wait-ForPort -Port $Service.Port -TimeoutSeconds 120)) {
        throw "Timed out waiting for $($Service.Name) to open port $($Service.Port).`n$((Get-LogTail -Service $Service) -join [Environment]::NewLine)"
    }

    if ($Service.RequiresNacos -and -not (Wait-ForNacosRegistration -ServiceName $Service.Name -TimeoutSeconds 60)) {
        throw "Timed out waiting for $($Service.Name) to register in Nacos.`n$((Get-LogTail -Service $Service) -join [Environment]::NewLine)"
    }

    return [pscustomobject]@{ Name = $Service.Name; Status = "started"; Port = $Service.Port }
}

function Stop-BackendService {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    $recordedPid = Get-RecordedPid -Service $Service
    if (-not $recordedPid) {
        Write-Host "[skip] $($Service.Name) has no recorded PID."
        return [pscustomobject]@{ Name = $Service.Name; Status = "not-managed"; Port = $Service.Port }
    }

    $process = Get-Process -Id $recordedPid -ErrorAction SilentlyContinue
    if (-not $process) {
        Clear-RecordedPid -Service $Service
        Write-Host "[clean] $($Service.Name) PID file removed; process was already gone."
        return [pscustomobject]@{ Name = $Service.Name; Status = "already-stopped"; Port = $Service.Port }
    }

    & taskkill.exe /PID $recordedPid /T /F | Out-Null
    if (-not (Wait-ForPortClosed -Port $Service.Port -TimeoutSeconds 30)) {
        throw "Timed out waiting for $($Service.Name) to stop listening on port $($Service.Port)."
    }

    Clear-RecordedPid -Service $Service
    Write-Host "[stop] $($Service.Name) stopped."
    return [pscustomobject]@{ Name = $Service.Name; Status = "stopped"; Port = $Service.Port }
}

function Get-BackendServiceStatus {
    param([Parameter(Mandatory = $true)][pscustomobject]$Service)

    $recordedPid = Get-RecordedPid -Service $Service
    $portOwner = Get-PortOwnerPid -Port $Service.Port
    $nacos = if ($Service.RequiresNacos) { Test-NacosRegistration -ServiceName $Service.Name } else { $null }

    return [pscustomobject]@{
        Name = $Service.Name
        Module = $Service.Module
        Port = $Service.Port
        PortListening = [bool]$portOwner
        PortPid = $portOwner
        ManagedPid = $recordedPid
        ManagedProcessAlive = if ($recordedPid) { Test-RecordedProcessAlive -Service $Service } else { $false }
        NacosRegistered = $nacos
        Log = Get-LogPath -Service $Service
    }
}
