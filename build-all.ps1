param(
    [switch]$SkipInstall
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot

function Resolve-CommandPath([string]$name, [string]$fallback) {
    $cmd = Get-Command $name -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    if ($fallback -and (Test-Path $fallback)) { return $fallback }
    throw "Command not found: $name. Please install it and add it to PATH."
}

$npm = Resolve-CommandPath 'npm.cmd' 'D:\Node\npm.cmd'
$mvn = Resolve-CommandPath 'mvn.cmd' (Join-Path $root '.tools\apache-maven-3.9.9\bin\mvn.cmd')

# Standalone jar is always served from the root path.
$env:VITE_BASE = '/'

Write-Host '==> Build frontend' -ForegroundColor Cyan
Push-Location (Join-Path $root 'frontend')
try {
    if (-not $SkipInstall -and -not (Test-Path 'node_modules')) {
        & $npm install
        if ($LASTEXITCODE -ne 0) { throw 'npm install failed' }
    }
    & $npm run build
    if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed' }
} finally {
    Pop-Location
}

Write-Host '==> Copy frontend assets to backend static' -ForegroundColor Cyan
$dist = Join-Path $root 'frontend\dist'
$static = Join-Path $root 'backend\src\main\resources\static'
if (-not (Test-Path $dist)) { throw 'frontend/dist not found, frontend build failed' }
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item -ItemType Directory -Path $static | Out-Null
Copy-Item (Join-Path $dist '*') $static -Recurse -Force

Write-Host '==> Package backend (frontend included)' -ForegroundColor Cyan
& $mvn -f (Join-Path $root 'backend\pom.xml') clean package -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'Backend package failed' }

$jar = Join-Path $root 'backend\target\archive-contract-system-1.0.0.jar'
Write-Host ''
Write-Host "Build complete: $jar" -ForegroundColor Green
Write-Host 'Start: java -jar backend\target\archive-contract-system-1.0.0.jar'
Write-Host 'Open:  http://localhost:8080/'
