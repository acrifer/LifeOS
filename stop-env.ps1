Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
Assert-RequiredPaths

$results = @(
    (Stop-RocketMqBroker)
    (Stop-RocketMqNameServer)
    (Stop-Nacos)
    (Stop-Redis)
    (Stop-DockerManagedDependency -Name "mysql")
)

Write-Host ""
Write-Host "Environment stop summary"
$results | Format-Table -AutoSize
Write-Host ""
Get-EnvironmentStatus | Format-Table Name, Port, Reachable, PortPid, ManagedMode, ManagedPid, ManagedProcessAlive -AutoSize
