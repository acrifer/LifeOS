Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
Assert-RequiredPaths

Write-Host "Environment status"
Get-EnvironmentStatus | Format-Table -AutoSize
Write-Host ""
Write-Host "Logs: $script:LogRoot"
