Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
Assert-RequiredPaths
Stop-RocketMQ
Stop-Nacos
Stop-Redis
Stop-DockerDependencies

Write-Host ""
Write-Host "Environment stop summary"
Get-EnvironmentStatus | Format-Table -AutoSize
