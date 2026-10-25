param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
if (!$SkipBuild) { & (Join-Path $PSScriptRoot 'build.ps1'); if (!$?) { throw 'Build failed.' } }
. (Join-Path $PSScriptRoot 'bootstrap.ps1')
$jar = Join-Path $projectRoot 'target\JavaAnime.jar'
if (!(Test-Path -LiteralPath $jar)) { throw 'Build the app before packaging.' }
$packageRoot = Join-Path $projectRoot ("target\portable-" + (Get-Date -Format 'yyyyMMdd-HHmmss'))
$inputRoot = Join-Path $packageRoot 'input'
$runtimeRoot = Join-Path $packageRoot 'runtime'
$outputRoot = Join-Path $packageRoot 'output'
$previewRoot = Join-Path $projectRoot 'target\visual-checks'
$distRoot = Join-Path $projectRoot 'dist'
New-Item -ItemType Directory -Path $inputRoot, $outputRoot, $distRoot -Force | Out-Null
New-Item -ItemType Directory -Path (Join-Path $inputRoot 'tools'), (Join-Path $inputRoot 'notices') -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination $inputRoot
Copy-Item -LiteralPath (Join-Path $toolsRoot 'ffmpeg\bin\ffmpeg.exe') -Destination (Join-Path $inputRoot 'tools')
Copy-Item -LiteralPath (Join-Path $toolsRoot 'ffmpeg\LICENSE') -Destination (Join-Path $inputRoot 'notices\FFmpeg-LICENSE.txt')
Copy-Item -LiteralPath (Join-Path $toolsRoot 'ffmpeg\README.txt') -Destination (Join-Path $inputRoot 'notices\FFmpeg-build.txt')
Copy-Item -LiteralPath (Join-Path $toolsRoot 'jdk\NOTICE') -Destination (Join-Path $inputRoot 'notices\Temurin-NOTICE.txt')
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'toolchain.json') -Destination (Join-Path $inputRoot 'notices')

& (Join-Path $toolsRoot 'jdk\bin\java.exe') '-Djava.awt.headless=true' -jar $jar --render-check $previewRoot
if ($LASTEXITCODE -ne 0) { throw 'Visual asset generation failed.' }
& (Join-Path $toolsRoot 'jdk\bin\jlink.exe') --add-modules java.desktop,java.logging --strip-debug --no-header-files --no-man-pages --compress=2 --output $runtimeRoot
if ($LASTEXITCODE -ne 0) { throw 'Runtime packaging failed.' }
& (Join-Path $toolsRoot 'jdk\bin\jpackage.exe') --type app-image --name JavaAnime --app-version 1.0.0 --vendor JavaAnime --description 'Offline text to cartoon animation studio' --input $inputRoot --main-jar JavaAnime.jar --main-class com.javaanime.Main --runtime-image $runtimeRoot --dest $outputRoot --icon (Join-Path $previewRoot 'JavaAnime.ico') --java-options '-Djavaanime.appDir=$APPDIR' --java-options '-Xmx512m'
if ($LASTEXITCODE -ne 0) { throw 'Windows application packaging failed.' }
$appRoot = Join-Path $outputRoot 'JavaAnime'
foreach ($file in @('README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md')) { Copy-Item -LiteralPath (Join-Path $projectRoot $file) -Destination $appRoot }

$sourceRoot = Join-Path $packageRoot 'source\JavaAnime'
New-Item -ItemType Directory -Path $sourceRoot -Force | Out-Null
foreach ($item in @('src', 'scripts', 'pom.xml', 'README.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md', '.gitignore')) {
    Copy-Item -LiteralPath (Join-Path $projectRoot $item) -Destination $sourceRoot -Recurse
}
$sourceZip = Join-Path $distRoot 'JavaAnime-1.0.0-source.zip'
Compress-Archive -LiteralPath $sourceRoot -DestinationPath $sourceZip -Force
Copy-Item -LiteralPath $sourceZip -Destination (Join-Path $appRoot 'JavaAnime-source.zip')
$portableZip = Join-Path $distRoot 'JavaAnime-1.0.0-windows-x64.zip'
Compress-Archive -LiteralPath $appRoot -DestinationPath $portableZip -Force
Write-Host "Portable app: $portableZip"
Write-Host "Source code: $sourceZip"
Write-Host "App folder: $appRoot"
