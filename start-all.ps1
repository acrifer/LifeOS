Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

. "$PSScriptRoot\_env-common.ps1"

Ensure-EnvironmentLayout
$envSnapshot = Get-EnvironmentManagementSnapshot

try {
    & "$PSScriptRoot\start-env.ps1"
    & "$PSScriptRoot\start-backend.ps1"
} catch {
    Write-Host "[rollback] start-all failed. Restoring environment state captured before startup..."
    try {
        & "$PSScriptRoot\stop-backend.ps1"
    } catch {
        Write-Warning "Backend rollback encountered an error: $($_.Exception.Message)"
    }

    try {
        [void](Restore-EnvironmentToSnapshot -Snapshot $envSnapshot)
    } catch {
        Write-Warning "Environment rollback encountered an error: $($_.Exception.Message)"
    }

    throw
}
