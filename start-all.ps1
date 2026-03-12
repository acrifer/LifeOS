Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

& "$PSScriptRoot\start-env.ps1"
& "$PSScriptRoot\start-backend.ps1"
