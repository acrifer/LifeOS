Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

& "$PSScriptRoot\status-env.ps1"
Write-Host ""
& "$PSScriptRoot\status-backend.ps1"
