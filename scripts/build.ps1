param([switch]$SkipTests)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'bootstrap.ps1')
Push-Location $projectRoot
try {
    $arguments = @('-B', '-ntp', '-Dmaven.repo.local=.tools/m2', 'package')
    if ($SkipTests) { $arguments += '-DskipTests' }
    & (Join-Path $toolsRoot 'maven\bin\mvn.cmd') @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Build or tests failed.' }
} finally { Pop-Location }
