$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$toolsRoot = Join-Path $projectRoot '.tools'
New-Item -ItemType Directory -Path $toolsRoot -Force | Out-Null
$lock = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'toolchain.json') -Raw | ConvertFrom-Json

foreach ($name in @('jdk', 'maven', 'ffmpeg')) {
    $spec = $lock.$name
    $destination = Join-Path $toolsRoot $name
    $marker = Join-Path $destination '.verified-version'
    if ((Test-Path -LiteralPath $marker) -and (Get-Content -LiteralPath $marker -Raw).Trim() -eq $spec.version) { continue }
    if (Test-Path -LiteralPath $destination) { throw "Unverified tool directory: $destination. Rename it before bootstrapping." }
    $archive = Join-Path $toolsRoot "$name-$($spec.version.Replace('+','_')).zip"
    if (!(Test-Path -LiteralPath $archive)) {
        Write-Host "Downloading $name $($spec.version)..."
        & curl.exe --fail --location --retry 3 --connect-timeout 30 --output "$archive.part" $spec.url
        if ($LASTEXITCODE -ne 0) { throw "Could not download $name." }
        Move-Item -LiteralPath "$archive.part" -Destination $archive
    }
    if ((Get-FileHash -LiteralPath $archive -Algorithm $spec.algorithm).Hash.ToLowerInvariant() -ne $spec.hash) {
        throw "Checksum failed for $archive. Remove this archive and try again."
    }
    $staging = Join-Path $toolsRoot "$name-extracted"
    if (Test-Path -LiteralPath $staging) { throw "Extraction directory already exists: $staging. Rename it before retrying." }
    New-Item -ItemType Directory -Path $staging | Out-Null
    & tar.exe -xf $archive -C $staging
    if ($LASTEXITCODE -ne 0) { throw "Could not extract $name." }
    $directory = @(Get-ChildItem -LiteralPath $staging -Directory)
    if ($directory.Count -ne 1) { throw "Unexpected archive structure for $name." }
    Move-Item -LiteralPath $directory[0].FullName -Destination $destination
    Set-Content -LiteralPath $marker -Value $spec.version -Encoding ascii
}
$env:JAVA_HOME = Join-Path $toolsRoot 'jdk'
$env:PATH = "$env:JAVA_HOME\bin;$(Join-Path $toolsRoot 'maven\bin');$env:PATH"
Write-Host 'Project-local Java, Maven, and video encoder are ready.'
