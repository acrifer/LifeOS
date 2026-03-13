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
$script:RuntimeRoot = Join-Path $script:ProjectRoot ".runtime"
$script:LogRoot = Join-Path $script:RuntimeRoot "logs"
$script:PidRoot = Join-Path $script:RuntimeRoot "pids"
$script:ComposeFile = Join-Path $script:ProjectRoot "docker-compose.infrastructure.yml"
$script:NacosRoot = Get-ConfigValue -Name "NACOS_HOME" -Default "C:\environment\nacos"
$script:RocketMqRoot = Get-ConfigValue -Name "ROCKETMQ_HOME" -Default "C:\environment\rocketmq-all-5.4.0-bin-release"
$script:MySqlHost = Get-ConfigValue -Name "MYSQL_HOST" -Default "127.0.0.1"
$script:MySqlPort = Get-ConfigIntValue -Name "MYSQL_PORT" -Default 3306
$script:RedisHost = Get-ConfigValue -Name "REDIS_HOST" -Default "127.0.0.1"
$script:RedisPort = Get-ConfigIntValue -Name "REDIS_PORT" -Default 6379
$script:NacosHost = Get-ConfigValue -Name "NACOS_HOST" -Default "127.0.0.1"
$script:NacosPort = Get-ConfigIntValue -Name "NACOS_PORT" -Default 8848
$script:NacosGrpcPort = Get-ConfigIntValue -Name "NACOS_GRPC_PORT" -Default 9848
$script:RocketMqNamesrvHost = Get-ConfigValue -Name "ROCKETMQ_NAMESRV_HOST" -Default "127.0.0.1"
$script:RocketMqNamesrvPort = Get-ConfigIntValue -Name "ROCKETMQ_NAMESRV_PORT" -Default 9876
$script:RocketMqBrokerPort = Get-ConfigIntValue -Name "ROCKETMQ_BROKER_PORT" -Default 10911
$script:DockerDependencies = @(
    [pscustomobject]@{ Name = "mysql"; DisplayName = "MySQL"; Service = "mysql"; Host = $script:MySqlHost; Port = $script:MySqlPort }
)

function Ensure-EnvironmentLayout {
    foreach ($path in @($script:RuntimeRoot, $script:LogRoot, $script:PidRoot)) {
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
        }
    }
}

function New-EnvironmentResult {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Status,
        [string]$Mode,
        [int]$Port = 0,
        [Nullable[int]]$ProcessId = $null
    )

    return [pscustomobject]@{
        Name = $Name
        Status = $Status
        Mode = $Mode
        Port = $Port
        Pid = $ProcessId
    }
}

function Get-EnvironmentLogPath {
    param([Parameter(Mandatory = $true)][string]$Name)

    return Join-Path $script:LogRoot $Name
}

function Get-EnvironmentPidPath {
    param([Parameter(Mandatory = $true)][string]$Name)

    return Join-Path $script:PidRoot ("env-" + $Name + ".pid")
}

function Get-EnvironmentModePath {
    param([Parameter(Mandatory = $true)][string]$Name)

    return Join-Path $script:PidRoot ("env-" + $Name + ".mode")
}

function Get-EnvironmentManagedMode {
    param([Parameter(Mandatory = $true)][string]$Name)

    $modePath = Get-EnvironmentModePath -Name $Name
    if (-not (Test-Path $modePath)) {
        return $null
    }

    $mode = (Get-Content -Raw $modePath).Trim()
    if (-not $mode) {
        Remove-Item $modePath -Force -ErrorAction SilentlyContinue
        return $null
    }

    return $mode
}

function Get-EnvironmentRecordedPid {
    param([Parameter(Mandatory = $true)][string]$Name)

    $pidPath = Get-EnvironmentPidPath -Name $Name
    if (-not (Test-Path $pidPath)) {
        return $null
    }

    $value = (Get-Content -Raw $pidPath).Trim()
    if (-not $value) {
        Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
        return $null
    }

    $parsedValue = 0
    if (-not [int]::TryParse($value, [ref]$parsedValue)) {
        Remove-Item $pidPath -Force -ErrorAction SilentlyContinue
        return $null
    }

    return $parsedValue
}

function Save-EnvironmentManagedState {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$Mode,
        [Nullable[int]]$ProcessId = $null
    )

    $Mode | Set-Content -Path (Get-EnvironmentModePath -Name $Name) -Encoding ASCII
    if ($null -ne $ProcessId) {
        $ProcessId | Set-Content -Path (Get-EnvironmentPidPath -Name $Name) -Encoding ASCII
    } else {
        Remove-Item (Get-EnvironmentPidPath -Name $Name) -Force -ErrorAction SilentlyContinue
    }
}

function Clear-EnvironmentManagedState {
    param([Parameter(Mandatory = $true)][string]$Name)

    Remove-Item (Get-EnvironmentModePath -Name $Name) -Force -ErrorAction SilentlyContinue
    Remove-Item (Get-EnvironmentPidPath -Name $Name) -Force -ErrorAction SilentlyContinue
}

function Test-EnvironmentRecordedProcessAlive {
    param([Parameter(Mandatory = $true)][string]$Name)

    $recordedPid = Get-EnvironmentRecordedPid -Name $Name
    if (-not $recordedPid) {
        return $false
    }

    $process = Get-Process -Id $recordedPid -ErrorAction SilentlyContinue
    if (-not $process) {
        Remove-Item (Get-EnvironmentPidPath -Name $Name) -Force -ErrorAction SilentlyContinue
        return $false
    }

    return $true
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
        [string]$Address = "127.0.0.1",
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-TcpPort -Address $Address -Port $Port) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ForPortClosed {
    param(
        [Parameter(Mandatory = $true)][int]$Port,
        [string]$Address = "127.0.0.1",
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-TcpPort -Address $Address -Port $Port)) {
            return $true
        }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Wait-ForHttpOk {
    param(
        [Parameter(Mandatory = $true)][string]$Uri,
        [int]$TimeoutSeconds = 90
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            Invoke-WebRequest -Uri $Uri -UseBasicParsing -TimeoutSec 5 | Out-Null
            return $true
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    return $false
}

function Assert-CommandAvailable {
    param([Parameter(Mandatory = $true)][string]$Name)

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "Required command '$Name' is not available."
    }
}

function Assert-DockerAvailable {
    Assert-CommandAvailable -Name "docker"
    & docker compose version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose is not available."
    }
    if (-not (Test-DockerEngineAvailable)) {
        throw "docker is installed, but the Docker engine is not running. Start Docker Desktop or provide a local dependency instead."
    }
}

function Test-DockerEngineAvailable {
    $dockerCommand = Get-Command "docker" -ErrorAction SilentlyContinue
    if (-not $dockerCommand) {
        return $false
    }

    & docker info | Out-Null
    return ($LASTEXITCODE -eq 0)
}

function Assert-JavaTooling {
    if (-not $env:JAVA_HOME) {
        throw "JAVA_HOME is not set. Nacos and RocketMQ require a JDK."
    }

    $javaExe = Join-Path $env:JAVA_HOME "bin\java.exe"
    $jpsExe = Join-Path $env:JAVA_HOME "bin\jps.exe"
    if (-not (Test-Path $javaExe)) {
        throw "JAVA_HOME does not contain java.exe: $javaExe"
    }
    if (-not (Test-Path $jpsExe)) {
        throw "JAVA_HOME does not contain jps.exe: $jpsExe"
    }
}

function Assert-RequiredPaths {
    foreach ($path in @($script:ComposeFile, $script:NacosRoot, $script:RocketMqRoot)) {
        if (-not (Test-Path $path)) {
            throw "Required path not found: $path"
        }
    }
}

function Invoke-Compose {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    Assert-DockerAvailable
    & docker compose -f $script:ComposeFile @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose failed: $($Arguments -join ' ')"
    }
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

function Get-DockerDependencyByName {
    param([Parameter(Mandatory = $true)][string]$Name)

    switch ($Name) {
        "mysql" {
            return $script:DockerDependencies | Where-Object { $_.Name -eq "mysql" }
        }
        "redis" {
            return [pscustomobject]@{ Name = "redis"; DisplayName = "Redis"; Service = "redis"; Host = $script:RedisHost; Port = $script:RedisPort }
        }
        default {
            throw "Unknown docker-managed dependency '$Name'."
        }
    }
}

function Start-DockerDependencies {
    $results = New-Object System.Collections.Generic.List[object]
    $servicesToStart = New-Object System.Collections.Generic.List[string]

    foreach ($dependency in $script:DockerDependencies) {
        if (Test-TcpPort -Address $dependency.Host -Port $dependency.Port) {
            Write-Host "[skip] $($dependency.DisplayName) is already reachable on port $($dependency.Port)."
            $results.Add((New-EnvironmentResult -Name $dependency.Name -Status "already-running" -Mode (Get-EnvironmentManagedMode -Name $dependency.Name) -Port $dependency.Port -ProcessId (Get-EnvironmentRecordedPid -Name $dependency.Name)))
        } else {
            $servicesToStart.Add($dependency.Service)
        }
    }

    if ($servicesToStart.Count -eq 0) {
        return $results
    }

    Write-Host "[docker] Starting: $($servicesToStart -join ', ')"
    $composeArguments = @("up", "-d") + $servicesToStart.ToArray()
    Invoke-Compose -Arguments $composeArguments

    foreach ($dependency in $script:DockerDependencies | Where-Object { $servicesToStart.Contains($_.Service) }) {
        if (-not (Wait-ForPort -Address $dependency.Host -Port $dependency.Port -TimeoutSeconds 120)) {
            throw "$($dependency.DisplayName) did not open port $($dependency.Port) in time."
        }

        Save-EnvironmentManagedState -Name $dependency.Name -Mode "docker"
        $results.Add((New-EnvironmentResult -Name $dependency.Name -Status "started" -Mode "docker" -Port $dependency.Port))
    }

    return $results
}

function Stop-DockerManagedDependency {
    param([Parameter(Mandatory = $true)][string]$Name)

    $dependency = Get-DockerDependencyByName -Name $Name
    $mode = Get-EnvironmentManagedMode -Name $Name
    if ($mode -ne "docker") {
        $status = if (Test-TcpPort -Address $dependency.Host -Port $dependency.Port) { "not-managed" } else { "already-stopped" }
        Write-Host "[skip] $($dependency.DisplayName) is not managed by this script."
        return New-EnvironmentResult -Name $Name -Status $status -Mode $mode -Port $dependency.Port -ProcessId (Get-EnvironmentRecordedPid -Name $Name)
    }

    if (-not (Test-DockerEngineAvailable)) {
        throw "Docker engine is not running; cannot stop managed dependency '$Name'."
    }

    Invoke-Compose -Arguments @("stop", $dependency.Service)
    if (-not (Wait-ForPortClosed -Address $dependency.Host -Port $dependency.Port -TimeoutSeconds 60)) {
        throw "$($dependency.DisplayName) did not release port $($dependency.Port) in time."
    }

    Clear-EnvironmentManagedState -Name $Name
    Write-Host "[stop] $($dependency.DisplayName) stopped."
    return New-EnvironmentResult -Name $Name -Status "stopped" -Mode "docker" -Port $dependency.Port
}

function Get-RedisServerPath {
    $command = Get-Command "redis-server.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    return $null
}

function Get-RedisCliPath {
    $command = Get-Command "redis-cli.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }
    return $null
}

function Test-RedisWritable {
    if (-not (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort)) {
        return $false
    }

    $redisCli = Get-RedisCliPath
    if (-not $redisCli) {
        return $true
    }

    $healthKey = "lifeos:redis:healthcheck"
    try {
        $ping = (& $redisCli -h $script:RedisHost -p $script:RedisPort ping 2>&1 | Out-String).Trim()
        if ($ping -notmatch "PONG") {
            return $false
        }

        $setResult = (& $redisCli -h $script:RedisHost -p $script:RedisPort set $healthKey "ok" ex 5 2>&1 | Out-String).Trim()
        if ($setResult -match "MISCONF|ERR|NOAUTH") {
            return $false
        }

        return $setResult -match "OK"
    } catch {
        return $false
    }
}

function Write-RedisRuntimeConfig {
    $redisRuntimeDir = Join-Path $script:RuntimeRoot "redis"
    if (-not (Test-Path $redisRuntimeDir)) {
        New-Item -ItemType Directory -Path $redisRuntimeDir -Force | Out-Null
    }

    $redisLogPath = Join-Path $script:LogRoot "redis.log"
    $configPath = Join-Path $redisRuntimeDir "redis-dev.conf"
    $configLines = @(
        "bind $($script:RedisHost)"
        "port $($script:RedisPort)"
        "dir $($redisRuntimeDir -replace '\\', '/')"
        "dbfilename dump.rdb"
        "appendonly no"
        'save ""'
        "stop-writes-on-bgsave-error no"
        "logfile $($redisLogPath -replace '\\', '/')"
    )
    $configLines | Set-Content -Path $configPath -Encoding ASCII
    return $configPath
}

function Start-Redis {
    if (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort) {
        $managedMode = Get-EnvironmentManagedMode -Name "redis"
        if ($managedMode -eq "local" -and -not (Test-RedisWritable)) {
            Write-Warning "Redis is reachable but not writable. Restarting the managed local instance."
            [void](Stop-Redis)
        } else {
        Write-Host "[skip] Redis is already reachable on port $($script:RedisPort)."
        return New-EnvironmentResult -Name "redis" -Status "already-running" -Mode $managedMode -Port $script:RedisPort -ProcessId (Get-EnvironmentRecordedPid -Name "redis")
        }
    }

    $redisServer = Get-RedisServerPath
    if ($redisServer) {
        $redisRuntimeDir = Join-Path $script:RuntimeRoot "redis"
        $redisConfig = Write-RedisRuntimeConfig
        $arguments = @("`"$redisConfig`"")

        $process = Start-Process -FilePath $redisServer `
            -ArgumentList $arguments `
            -WorkingDirectory $redisRuntimeDir `
            -WindowStyle Hidden `
            -PassThru

        if (-not (Wait-ForPort -Address $script:RedisHost -Port $script:RedisPort -TimeoutSeconds 30)) {
            throw "Redis did not open port $($script:RedisPort) in time."
        }

        $processId = Get-PortOwnerPid -Port $script:RedisPort
        if (-not $processId) {
            $processId = $process.Id
        }
        Save-EnvironmentManagedState -Name "redis" -Mode "local" -ProcessId $processId
        Write-Host "[start] Redis started from local installation."
        return New-EnvironmentResult -Name "redis" -Status "started" -Mode "local" -Port $script:RedisPort -ProcessId $processId
    }

    Write-Host "[docker] Redis local executable not found, falling back to Docker."
    Invoke-Compose -Arguments @("up", "-d", "redis")
    if (-not (Wait-ForPort -Address $script:RedisHost -Port $script:RedisPort -TimeoutSeconds 60)) {
        throw "Redis did not open port $($script:RedisPort) in time."
    }

    Save-EnvironmentManagedState -Name "redis" -Mode "docker"
    return New-EnvironmentResult -Name "redis" -Status "started" -Mode "docker" -Port $script:RedisPort
}

function Stop-Redis {
    $mode = Get-EnvironmentManagedMode -Name "redis"
    if (-not $mode) {
        $status = if (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort) { "not-managed" } else { "already-stopped" }
        Write-Host "[skip] Redis is not managed by this script."
        return New-EnvironmentResult -Name "redis" -Status $status -Port $script:RedisPort
    }

    if ($mode -eq "docker") {
        return Stop-DockerManagedDependency -Name "redis"
    }

    $recordedPid = Get-EnvironmentRecordedPid -Name "redis"
    $portOwner = Get-PortOwnerPid -Port $script:RedisPort
    $redisCli = Get-RedisCliPath
    if ($recordedPid -and (Get-Process -Id $recordedPid -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $recordedPid /T /F | Out-Null
    } elseif ($redisCli -and (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort)) {
        & $redisCli -h $script:RedisHost -p $script:RedisPort shutdown | Out-Null
    } elseif ($portOwner) {
        & taskkill.exe /PID $portOwner /T /F | Out-Null
    }

    if (-not (Wait-ForPortClosed -Address $script:RedisHost -Port $script:RedisPort -TimeoutSeconds 30)) {
        throw "Redis did not stop listening on port $($script:RedisPort) in time."
    }

    Clear-EnvironmentManagedState -Name "redis"
    Write-Host "[stop] Redis stopped."
    return New-EnvironmentResult -Name "redis" -Status "stopped" -Mode "local" -Port $script:RedisPort -ProcessId $recordedPid
}

function Start-Nacos {
    Assert-JavaTooling

    if ((Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort) -and (Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort)) {
        Write-Host "[skip] Nacos is already reachable on ports $($script:NacosPort)/$($script:NacosGrpcPort)."
        return New-EnvironmentResult -Name "nacos" -Status "already-running" -Mode (Get-EnvironmentManagedMode -Name "nacos") -Port $script:NacosPort -ProcessId (Get-EnvironmentRecordedPid -Name "nacos")
    }

    $binDir = Join-Path $script:NacosRoot "bin"
    $logPath = Get-EnvironmentLogPath -Name "nacos-console.log"
    Set-Content -Path $logPath -Value "" -Encoding ASCII

    $command = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && ' +
        'set "CUSTOM_NACOS_MEMORY=-Xms512m -Xmx512m -Xmn256m" && ' +
        'call startup.cmd -m standalone > "' + $logPath + '" 2>&1'

    Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", $command) `
        -WorkingDirectory $binDir `
        -WindowStyle Hidden | Out-Null

    if (-not (Wait-ForPort -Address $script:NacosHost -Port $script:NacosPort -TimeoutSeconds 120)) {
        throw "Nacos HTTP port $($script:NacosPort) did not open in time. Check $logPath"
    }
    if (-not (Wait-ForPort -Address $script:NacosHost -Port $script:NacosGrpcPort -TimeoutSeconds 120)) {
        throw "Nacos gRPC port $($script:NacosGrpcPort) did not open in time. Check $logPath"
    }
    [void](Wait-ForHttpOk -Uri "http://$($script:NacosHost):$($script:NacosPort)/nacos/" -TimeoutSeconds 30)
    Start-Sleep -Seconds 10

    $processId = Get-PortOwnerPid -Port $script:NacosPort
    Save-EnvironmentManagedState -Name "nacos" -Mode "local" -ProcessId $processId
    Write-Host "[start] Nacos started."
    return New-EnvironmentResult -Name "nacos" -Status "started" -Mode "local" -Port $script:NacosPort -ProcessId $processId
}

function Stop-Nacos {
    Assert-JavaTooling

    $mode = Get-EnvironmentManagedMode -Name "nacos"
    if (-not $mode) {
        $status = if ((Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort) -or (Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort)) { "not-managed" } else { "already-stopped" }
        Write-Host "[skip] Nacos is not managed by this script."
        return New-EnvironmentResult -Name "nacos" -Status $status -Port $script:NacosPort
    }

    $recordedPid = Get-EnvironmentRecordedPid -Name "nacos"
    if ($recordedPid -and (Get-Process -Id $recordedPid -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $recordedPid /T /F | Out-Null
    } else {
        $binDir = Join-Path $script:NacosRoot "bin"
        $command = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call shutdown.cmd'
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $command) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden `
            -Wait | Out-Null
    }

    if (-not (Wait-ForPortClosed -Address $script:NacosHost -Port $script:NacosPort -TimeoutSeconds 60)) {
        throw "Nacos HTTP port $($script:NacosPort) did not close in time."
    }
    if (-not (Wait-ForPortClosed -Address $script:NacosHost -Port $script:NacosGrpcPort -TimeoutSeconds 60)) {
        throw "Nacos gRPC port $($script:NacosGrpcPort) did not close in time."
    }

    Clear-EnvironmentManagedState -Name "nacos"
    Write-Host "[stop] Nacos stopped."
    return New-EnvironmentResult -Name "nacos" -Status "stopped" -Mode $mode -Port $script:NacosPort -ProcessId $recordedPid
}

function Start-RocketMQ {
    Assert-JavaTooling

    $results = New-Object System.Collections.Generic.List[object]
    $binDir = Join-Path $script:RocketMqRoot "bin"
    $namesrvLog = Get-EnvironmentLogPath -Name "rocketmq-namesrv-console.log"
    $brokerLog = Get-EnvironmentLogPath -Name "rocketmq-broker-console.log"

    if (-not (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort)) {
        Set-Content -Path $namesrvLog -Value "" -Encoding ASCII
        $namesrvCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && ' +
            'set "ROCKETMQ_HOME=' + $script:RocketMqRoot + '" && ' +
            'set "JAVA_OPT_EXT=-Xms512m -Xmx512m -Xmn256m -XX:MaxDirectMemorySize=512m" && ' +
            'call mqnamesrv.cmd > "' + $namesrvLog + '" 2>&1'

        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $namesrvCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden | Out-Null

        if (-not (Wait-ForPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort -TimeoutSeconds 120)) {
            throw "RocketMQ NameServer did not open port $($script:RocketMqNamesrvPort) in time. Check $namesrvLog"
        }
        $namesrvPid = Get-PortOwnerPid -Port $script:RocketMqNamesrvPort
        Save-EnvironmentManagedState -Name "rocketmq-namesrv" -Mode "local" -ProcessId $namesrvPid
        Write-Host "[start] RocketMQ NameServer started."
        $results.Add((New-EnvironmentResult -Name "rocketmq-namesrv" -Status "started" -Mode "local" -Port $script:RocketMqNamesrvPort -ProcessId $namesrvPid))
    } else {
        Write-Host "[skip] RocketMQ NameServer is already reachable on port $($script:RocketMqNamesrvPort)."
        $results.Add((New-EnvironmentResult -Name "rocketmq-namesrv" -Status "already-running" -Mode (Get-EnvironmentManagedMode -Name "rocketmq-namesrv") -Port $script:RocketMqNamesrvPort -ProcessId (Get-EnvironmentRecordedPid -Name "rocketmq-namesrv")))
    }

    if (-not (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort)) {
        Set-Content -Path $brokerLog -Value "" -Encoding ASCII
        $brokerCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && ' +
            'set "ROCKETMQ_HOME=' + $script:RocketMqRoot + '" && ' +
            'set "JAVA_OPT_EXT=-Xms512m -Xmx512m -Xmn256m -XX:MaxDirectMemorySize=512m" && ' +
            'call mqbroker.cmd -n ' + $script:RocketMqNamesrvHost + ':' + $script:RocketMqNamesrvPort + ' -c "..\conf\broker.conf" > "' + $brokerLog + '" 2>&1'

        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $brokerCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden | Out-Null

        if (-not (Wait-ForPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort -TimeoutSeconds 120)) {
            throw "RocketMQ Broker did not open port $($script:RocketMqBrokerPort) in time. Check $brokerLog"
        }
        $brokerPid = Get-PortOwnerPid -Port $script:RocketMqBrokerPort
        Save-EnvironmentManagedState -Name "rocketmq-broker" -Mode "local" -ProcessId $brokerPid
        Write-Host "[start] RocketMQ Broker started."
        $results.Add((New-EnvironmentResult -Name "rocketmq-broker" -Status "started" -Mode "local" -Port $script:RocketMqBrokerPort -ProcessId $brokerPid))
    } else {
        Write-Host "[skip] RocketMQ Broker is already reachable on port $($script:RocketMqBrokerPort)."
        $results.Add((New-EnvironmentResult -Name "rocketmq-broker" -Status "already-running" -Mode (Get-EnvironmentManagedMode -Name "rocketmq-broker") -Port $script:RocketMqBrokerPort -ProcessId (Get-EnvironmentRecordedPid -Name "rocketmq-broker")))
    }

    return $results
}

function Stop-RocketMqBroker {
    Assert-JavaTooling

    $mode = Get-EnvironmentManagedMode -Name "rocketmq-broker"
    if (-not $mode) {
        $status = if (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort) { "not-managed" } else { "already-stopped" }
        Write-Host "[skip] RocketMQ Broker is not managed by this script."
        return New-EnvironmentResult -Name "rocketmq-broker" -Status $status -Port $script:RocketMqBrokerPort
    }

    $recordedPid = Get-EnvironmentRecordedPid -Name "rocketmq-broker"
    if ($recordedPid -and (Get-Process -Id $recordedPid -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $recordedPid /T /F | Out-Null
    } else {
        $binDir = Join-Path $script:RocketMqRoot "bin"
        $brokerCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call mqshutdown.cmd broker'
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $brokerCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden `
            -Wait | Out-Null
    }

    if (-not (Wait-ForPortClosed -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort -TimeoutSeconds 60)) {
        throw "RocketMQ Broker did not release port $($script:RocketMqBrokerPort) in time."
    }

    Clear-EnvironmentManagedState -Name "rocketmq-broker"
    Write-Host "[stop] RocketMQ Broker stopped."
    return New-EnvironmentResult -Name "rocketmq-broker" -Status "stopped" -Mode $mode -Port $script:RocketMqBrokerPort -ProcessId $recordedPid
}

function Stop-RocketMqNameServer {
    Assert-JavaTooling

    $mode = Get-EnvironmentManagedMode -Name "rocketmq-namesrv"
    if (-not $mode) {
        $status = if (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort) { "not-managed" } else { "already-stopped" }
        Write-Host "[skip] RocketMQ NameServer is not managed by this script."
        return New-EnvironmentResult -Name "rocketmq-namesrv" -Status $status -Port $script:RocketMqNamesrvPort
    }

    $recordedPid = Get-EnvironmentRecordedPid -Name "rocketmq-namesrv"
    if ($recordedPid -and (Get-Process -Id $recordedPid -ErrorAction SilentlyContinue)) {
        & taskkill.exe /PID $recordedPid /T /F | Out-Null
    } else {
        $binDir = Join-Path $script:RocketMqRoot "bin"
        $namesrvCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call mqshutdown.cmd namesrv'
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $namesrvCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden `
            -Wait | Out-Null
    }

    if (-not (Wait-ForPortClosed -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort -TimeoutSeconds 60)) {
        throw "RocketMQ NameServer did not release port $($script:RocketMqNamesrvPort) in time."
    }

    Clear-EnvironmentManagedState -Name "rocketmq-namesrv"
    Write-Host "[stop] RocketMQ NameServer stopped."
    return New-EnvironmentResult -Name "rocketmq-namesrv" -Status "stopped" -Mode $mode -Port $script:RocketMqNamesrvPort -ProcessId $recordedPid
}

function Get-EnvironmentManagementSnapshot {
    return @{
        "mysql" = [pscustomobject]@{ Mode = (Get-EnvironmentManagedMode -Name "mysql"); Pid = (Get-EnvironmentRecordedPid -Name "mysql") }
        "redis" = [pscustomobject]@{ Mode = (Get-EnvironmentManagedMode -Name "redis"); Pid = (Get-EnvironmentRecordedPid -Name "redis") }
        "nacos" = [pscustomobject]@{ Mode = (Get-EnvironmentManagedMode -Name "nacos"); Pid = (Get-EnvironmentRecordedPid -Name "nacos") }
        "rocketmq-namesrv" = [pscustomobject]@{ Mode = (Get-EnvironmentManagedMode -Name "rocketmq-namesrv"); Pid = (Get-EnvironmentRecordedPid -Name "rocketmq-namesrv") }
        "rocketmq-broker" = [pscustomobject]@{ Mode = (Get-EnvironmentManagedMode -Name "rocketmq-broker"); Pid = (Get-EnvironmentRecordedPid -Name "rocketmq-broker") }
    }
}

function Test-EnvironmentStateChangedFromSnapshot {
    param(
        [Parameter(Mandatory = $true)][hashtable]$Snapshot,
        [Parameter(Mandatory = $true)][string]$Name
    )

    $previous = $Snapshot[$Name]
    $currentMode = Get-EnvironmentManagedMode -Name $Name
    $currentPid = Get-EnvironmentRecordedPid -Name $Name

    if (-not $previous) {
        return ($null -ne $currentMode) -or ($null -ne $currentPid)
    }

    if ($currentMode -ne $previous.Mode) {
        return $true
    }

    if ($currentMode -eq "local" -and $currentPid -ne $previous.Pid) {
        return $true
    }

    return $false
}

function Stop-EnvironmentComponent {
    param([Parameter(Mandatory = $true)][string]$Name)

    switch ($Name) {
        "rocketmq-broker" { return Stop-RocketMqBroker }
        "rocketmq-namesrv" { return Stop-RocketMqNameServer }
        "nacos" { return Stop-Nacos }
        "redis" { return Stop-Redis }
        "mysql" { return Stop-DockerManagedDependency -Name "mysql" }
        default { throw "Unknown environment component '$Name'." }
    }
}

function Restore-EnvironmentToSnapshot {
    param([Parameter(Mandatory = $true)][hashtable]$Snapshot)

    $results = New-Object System.Collections.Generic.List[object]
    foreach ($name in @("rocketmq-broker", "rocketmq-namesrv", "nacos", "redis", "mysql")) {
        if (Test-EnvironmentStateChangedFromSnapshot -Snapshot $Snapshot -Name $name) {
            $results.Add((Stop-EnvironmentComponent -Name $name))
        }
    }

    return $results
}

function Get-EnvironmentStatus {
    $rows = @(
        [pscustomobject]@{
            Name = "MySQL"
            Component = "mysql"
            Port = $script:MySqlPort
            Reachable = (Test-TcpPort -Address $script:MySqlHost -Port $script:MySqlPort)
            PortPid = (Get-PortOwnerPid -Port $script:MySqlPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "mysql")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "mysql")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "mysql")
        },
        [pscustomobject]@{
            Name = "Redis"
            Component = "redis"
            Port = $script:RedisPort
            Reachable = (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort)
            PortPid = (Get-PortOwnerPid -Port $script:RedisPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "redis")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "redis")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "redis")
        },
        [pscustomobject]@{
            Name = "Nacos HTTP"
            Component = "nacos"
            Port = $script:NacosPort
            Reachable = (Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort)
            PortPid = (Get-PortOwnerPid -Port $script:NacosPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "nacos")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "nacos")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "nacos")
        },
        [pscustomobject]@{
            Name = "Nacos gRPC"
            Component = "nacos"
            Port = $script:NacosGrpcPort
            Reachable = (Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort)
            PortPid = (Get-PortOwnerPid -Port $script:NacosGrpcPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "nacos")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "nacos")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "nacos")
        },
        [pscustomobject]@{
            Name = "RocketMQ NameServer"
            Component = "rocketmq-namesrv"
            Port = $script:RocketMqNamesrvPort
            Reachable = (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort)
            PortPid = (Get-PortOwnerPid -Port $script:RocketMqNamesrvPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "rocketmq-namesrv")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "rocketmq-namesrv")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "rocketmq-namesrv")
        },
        [pscustomobject]@{
            Name = "RocketMQ Broker"
            Component = "rocketmq-broker"
            Port = $script:RocketMqBrokerPort
            Reachable = (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort)
            PortPid = (Get-PortOwnerPid -Port $script:RocketMqBrokerPort)
            ManagedMode = (Get-EnvironmentManagedMode -Name "rocketmq-broker")
            ManagedPid = (Get-EnvironmentRecordedPid -Name "rocketmq-broker")
            ManagedProcessAlive = (Test-EnvironmentRecordedProcessAlive -Name "rocketmq-broker")
        }
    )

    return $rows
}
