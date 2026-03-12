Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_backend-common.ps1"

Ensure-RuntimeLayout

$dependencyRows = @(
    [pscustomobject]@{ Dependency = "MySQL"; Address = "127.0.0.1"; Port = 3306; Reachable = Test-TcpPort -Address "127.0.0.1" -Port 3306 },
    [pscustomobject]@{ Dependency = "Redis"; Address = "127.0.0.1"; Port = 6379; Reachable = Test-TcpPort -Address "127.0.0.1" -Port 6379 },
    [pscustomobject]@{ Dependency = "Nacos HTTP"; Address = "127.0.0.1"; Port = 8848; Reachable = Test-TcpPort -Address "127.0.0.1" -Port 8848 },
    [pscustomobject]@{ Dependency = "Nacos gRPC"; Address = "127.0.0.1"; Port = 9848; Reachable = Test-TcpPort -Address "127.0.0.1" -Port 9848 }
)

$serviceRows = foreach ($service in Get-ServiceDefinitions) {
    Get-BackendServiceStatus -Service $service
}

Write-Host "Dependency status"
$dependencyRows | Format-Table -AutoSize
Write-Host ""
Write-Host "Backend service status"
$serviceRows | Format-Table Name, Port, PortListening, PortPid, ManagedPid, ManagedProcessAlive, NacosRegistered -AutoSize
Write-Host ""
Write-Host "Logs: $script:LogRoot"
