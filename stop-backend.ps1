Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_backend-common.ps1"

Ensure-RuntimeLayout

$results = New-Object System.Collections.Generic.List[object]

foreach ($service in (Get-ServiceDefinitions | Sort-Object Port -Descending)) {
    $results.Add((Stop-BackendService -Service $service))
}

Write-Host ""
Write-Host "Backend stop summary"
$results | Format-Table -AutoSize

