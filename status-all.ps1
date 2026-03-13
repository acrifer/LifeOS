Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$errors = New-Object System.Collections.Generic.List[string]

try {
    & "$PSScriptRoot\status-env.ps1"
} catch {
    $errors.Add("status-env.ps1: $($_.Exception.Message)")
}

Write-Host ""

try {
    & "$PSScriptRoot\status-backend.ps1"
} catch {
    $errors.Add("status-backend.ps1: $($_.Exception.Message)")
}

if ($errors.Count -gt 0) {
    throw ($errors -join [Environment]::NewLine)
}
