Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_backend-common.ps1"

Ensure-RuntimeLayout
Assert-Dependencies
Build-SharedModules

$results = New-Object System.Collections.Generic.List[object]

foreach ($service in Get-ServiceDefinitions) {
    $results.Add((Start-BackendService -Service $service))
}

Write-Host ""
Write-Host "Backend startup summary"
Write-Host "Logs: $script:LogRoot"
$results | Format-Table -AutoSize
