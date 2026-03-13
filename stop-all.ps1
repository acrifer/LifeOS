Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$errors = New-Object System.Collections.Generic.List[string]

try {
    & "$PSScriptRoot\stop-backend.ps1"
} catch {
    $errors.Add("stop-backend.ps1: $($_.Exception.Message)")
}

try {
    & "$PSScriptRoot\stop-env.ps1"
} catch {
    $errors.Add("stop-env.ps1: $($_.Exception.Message)")
}

if ($errors.Count -gt 0) {
    throw ($errors -join [Environment]::NewLine)
}
