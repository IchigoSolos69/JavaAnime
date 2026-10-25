$ErrorActionPreference = 'Stop'
& (Join-Path $PSScriptRoot 'build.ps1')
if (!$?) { throw 'Build failed.' }
$projectRoot = Split-Path -Parent $PSScriptRoot
& (Join-Path $projectRoot '.tools\jdk\bin\java.exe') "-Djavaanime.ffmpeg=$(Join-Path $projectRoot '.tools\ffmpeg\bin\ffmpeg.exe')" -jar (Join-Path $projectRoot 'target\JavaAnime.jar')
