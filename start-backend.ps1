Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_backend-common.ps1"

Ensure-RuntimeLayout
Assert-Dependencies
Build-SharedModules

$results = New-Object System.Collections.Generic.List[object]
$startedServices = New-Object System.Collections.Generic.List[object]

try {
    foreach ($service in Get-ServiceDefinitions) {
        $result = Start-BackendService -Service $service
        $results.Add($result)
        if ($result.Status -eq "started") {
            $startedServices.Add($service)
        }
    }
} catch {
    Write-Host "[rollback] Backend startup failed. Stopping services started in this run..."
    foreach ($service in ($startedServices | Sort-Object Port -Descending)) {
        try {
            [void](Stop-BackendService -Service $service)
        } catch {
            Write-Warning "Failed to stop $($service.Name) during rollback: $($_.Exception.Message)"
        }
    }
    throw
}

Write-Host ""
Write-Host "Backend startup summary"
Write-Host "Logs: $script:LogRoot"
$results | Format-Table -AutoSize
