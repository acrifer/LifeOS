Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

& "$PSScriptRoot\stop-backend.ps1"
& "$PSScriptRoot\stop-env.ps1"
