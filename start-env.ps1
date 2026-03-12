Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
Assert-RequiredPaths
Start-DockerDependencies
Start-Redis
Start-Nacos
Start-RocketMQ

Write-Host ""
Write-Host "Environment startup summary"
Get-EnvironmentStatus | Format-Table -AutoSize
Write-Host ""
Write-Host "Logs: $script:LogRoot"
