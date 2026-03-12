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
    [pscustomobject]@{ Name = "MySQL"; Service = "mysql"; Host = $script:MySqlHost; Port = $script:MySqlPort }
)

function Ensure-EnvironmentLayout {
    foreach ($path in @($script:RuntimeRoot, $script:LogRoot)) {
        if (-not (Test-Path $path)) {
            New-Item -ItemType Directory -Path $path -Force | Out-Null
        }
    }
}

function Get-EnvironmentLogPath {
    param([Parameter(Mandatory = $true)][string]$Name)

    return Join-Path $script:LogRoot $Name
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
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (-not (Test-TcpPort -Address "127.0.0.1" -Port $Port)) {
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

function Start-DockerDependencies {
    $servicesToStart = New-Object System.Collections.Generic.List[string]

    foreach ($dependency in $script:DockerDependencies) {
        if (Test-TcpPort -Address $dependency.Host -Port $dependency.Port) {
            Write-Host "[skip] $($dependency.Name) is already reachable on port $($dependency.Port)."
        } else {
            $servicesToStart.Add($dependency.Service)
        }
    }

    if ($servicesToStart.Count -eq 0) {
        return
    }

    Write-Host "[docker] Starting: $($servicesToStart -join ', ')"
    $composeArguments = @("up", "-d") + $servicesToStart.ToArray()
    Invoke-Compose -Arguments $composeArguments

    foreach ($dependency in $script:DockerDependencies | Where-Object { $servicesToStart.Contains($_.Service) }) {
        if (-not (Wait-ForPort -Port $dependency.Port -TimeoutSeconds 120)) {
            throw "$($dependency.Name) did not open port $($dependency.Port) in time."
        }
    }
}

function Stop-DockerDependencies {
    if (-not (Test-DockerEngineAvailable)) {
        Write-Host "[skip] Docker engine is not running; docker-managed dependencies were not stopped."
        return
    }

    Invoke-Compose -Arguments @("stop", "mysql")
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

function Start-Redis {
    if (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort) {
        Write-Host "[skip] Redis is already reachable on port $($script:RedisPort)."
        return
    }

    $redisServer = Get-RedisServerPath
    if ($redisServer) {
        $redisDir = Split-Path -Parent $redisServer
        $redisConfig = Join-Path $redisDir "redis.windows.conf"
        $arguments = if (Test-Path $redisConfig) {
            @("`"$redisConfig`"")
        } else {
            @("--port", "$($script:RedisPort)")
        }

        Start-Process -FilePath $redisServer `
            -ArgumentList $arguments `
            -WorkingDirectory $redisDir `
            -WindowStyle Hidden | Out-Null

        if (-not (Wait-ForPort -Port $script:RedisPort -TimeoutSeconds 30)) {
            throw "Redis did not open port $($script:RedisPort) in time."
        }
        Write-Host "[start] Redis started from local installation."
        return
    }

    Write-Host "[docker] Redis local executable not found, falling back to Docker."
    Invoke-Compose -Arguments @("up", "-d", "redis")
    if (-not (Wait-ForPort -Port $script:RedisPort -TimeoutSeconds 60)) {
        throw "Redis did not open port $($script:RedisPort) in time."
    }
}

function Stop-Redis {
    if (-not (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort)) {
        Write-Host "[skip] Redis is not listening."
        return
    }

    $redisCli = Get-RedisCliPath
    if ($redisCli) {
        & $redisCli -h $script:RedisHost -p $script:RedisPort shutdown | Out-Null
        Start-Sleep -Seconds 2
    }

    if (Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort) {
        Get-Process -Name "redis-server" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
    }

    [void](Wait-ForPortClosed -Port $script:RedisPort -TimeoutSeconds 30)
    Write-Host "[stop] Redis stop requested."
}

function Start-Nacos {
    Assert-JavaTooling

    if ((Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort) -and (Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort)) {
        Write-Host "[skip] Nacos is already reachable on ports $($script:NacosPort)/$($script:NacosGrpcPort)."
        return
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

    if (-not (Wait-ForPort -Port $script:NacosPort -TimeoutSeconds 120)) {
        throw "Nacos HTTP port $($script:NacosPort) did not open in time. Check $logPath"
    }
    if (-not (Wait-ForPort -Port $script:NacosGrpcPort -TimeoutSeconds 120)) {
        throw "Nacos gRPC port $($script:NacosGrpcPort) did not open in time. Check $logPath"
    }
    [void](Wait-ForHttpOk -Uri "http://$($script:NacosHost):$($script:NacosPort)/nacos/" -TimeoutSeconds 30)

    Write-Host "[start] Nacos started."
}

function Stop-Nacos {
    Assert-JavaTooling

    if (-not (Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort) -and -not (Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort)) {
        Write-Host "[skip] Nacos is not listening."
        return
    }

    $binDir = Join-Path $script:NacosRoot "bin"
    $command = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call shutdown.cmd'
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList @("/c", $command) `
        -WorkingDirectory $binDir `
        -WindowStyle Hidden `
        -Wait | Out-Null

    [void](Wait-ForPortClosed -Port $script:NacosPort -TimeoutSeconds 60)
    [void](Wait-ForPortClosed -Port $script:NacosGrpcPort -TimeoutSeconds 60)
    Write-Host "[stop] Nacos stop requested."
}

function Start-RocketMQ {
    Assert-JavaTooling

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

        if (-not (Wait-ForPort -Port $script:RocketMqNamesrvPort -TimeoutSeconds 120)) {
            throw "RocketMQ NameServer did not open port $($script:RocketMqNamesrvPort) in time. Check $namesrvLog"
        }
        Write-Host "[start] RocketMQ NameServer started."
    } else {
        Write-Host "[skip] RocketMQ NameServer is already reachable on port $($script:RocketMqNamesrvPort)."
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

        if (-not (Wait-ForPort -Port $script:RocketMqBrokerPort -TimeoutSeconds 120)) {
            throw "RocketMQ Broker did not open port $($script:RocketMqBrokerPort) in time. Check $brokerLog"
        }
        Write-Host "[start] RocketMQ Broker started."
    } else {
        Write-Host "[skip] RocketMQ Broker is already reachable on port $($script:RocketMqBrokerPort)."
    }
}

function Stop-RocketMQ {
    Assert-JavaTooling

    $binDir = Join-Path $script:RocketMqRoot "bin"

    if (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort) {
        $brokerCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call mqshutdown.cmd broker'
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $brokerCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden `
            -Wait | Out-Null
        [void](Wait-ForPortClosed -Port $script:RocketMqBrokerPort -TimeoutSeconds 60)
        Write-Host "[stop] RocketMQ Broker stop requested."
    } else {
        Write-Host "[skip] RocketMQ Broker is not listening."
    }

    if (Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort) {
        $namesrvCommand = 'set "JAVA_HOME=' + $env:JAVA_HOME + '" && call mqshutdown.cmd namesrv'
        Start-Process -FilePath "cmd.exe" `
            -ArgumentList @("/c", $namesrvCommand) `
            -WorkingDirectory $binDir `
            -WindowStyle Hidden `
            -Wait | Out-Null
        [void](Wait-ForPortClosed -Port $script:RocketMqNamesrvPort -TimeoutSeconds 60)
        Write-Host "[stop] RocketMQ NameServer stop requested."
    } else {
        Write-Host "[skip] RocketMQ NameServer is not listening."
    }
}

function Get-EnvironmentStatus {
    $rows = @(
        [pscustomobject]@{ Name = "MySQL"; Port = $script:MySqlPort; Reachable = Test-TcpPort -Address $script:MySqlHost -Port $script:MySqlPort },
        [pscustomobject]@{ Name = "Redis"; Port = $script:RedisPort; Reachable = Test-TcpPort -Address $script:RedisHost -Port $script:RedisPort },
        [pscustomobject]@{ Name = "Nacos HTTP"; Port = $script:NacosPort; Reachable = Test-TcpPort -Address $script:NacosHost -Port $script:NacosPort },
        [pscustomobject]@{ Name = "Nacos gRPC"; Port = $script:NacosGrpcPort; Reachable = Test-TcpPort -Address $script:NacosHost -Port $script:NacosGrpcPort },
        [pscustomobject]@{ Name = "RocketMQ NameServer"; Port = $script:RocketMqNamesrvPort; Reachable = Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqNamesrvPort },
        [pscustomobject]@{ Name = "RocketMQ Broker"; Port = $script:RocketMqBrokerPort; Reachable = Test-TcpPort -Address $script:RocketMqNamesrvHost -Port $script:RocketMqBrokerPort }
    )

    return $rows
}
