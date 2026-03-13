Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
Assert-RequiredPaths

$snapshot = Get-EnvironmentManagementSnapshot
$results = New-Object System.Collections.Generic.List[object]

try {
    foreach ($item in (Start-DockerDependencies)) {
        $results.Add($item)
    }
    $results.Add((Start-Redis))
    $results.Add((Start-Nacos))
    foreach ($item in (Start-RocketMQ)) {
        $results.Add($item)
    }
} catch {
    Write-Host "[rollback] Environment startup failed. Reverting components started in this run..."
    [void](Restore-EnvironmentToSnapshot -Snapshot $snapshot)
    throw
}

Write-Host ""
Write-Host "Environment startup summary"
$results | Format-Table -AutoSize
Write-Host ""
Get-EnvironmentStatus | Format-Table Name, Port, Reachable, PortPid, ManagedMode, ManagedPid, ManagedProcessAlive -AutoSize
Write-Host ""
Write-Host "Logs: $script:LogRoot"
