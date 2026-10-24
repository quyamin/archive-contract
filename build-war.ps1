param(
    [string]$ContextPath = 'archive',
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

$name = $ContextPath.Trim().Trim('/')
if ([string]::IsNullOrWhiteSpace($name) -or $name -eq 'ROOT') {
    $base = '/'
    $warName = 'ROOT'
} else {
    if ($name -notmatch '^[A-Za-z0-9_-]+$') {
        throw "ContextPath may only contain letters, digits, '-' and '_': $ContextPath"
    }
    $base = "/$name/"
    $warName = $name
}

Write-Host "==> Frontend base: $base"
$env:VITE_BASE = $base

Push-Location (Join-Path $root 'frontend')
try {
    if (-not $SkipInstall) {
        & $npm install
        if ($LASTEXITCODE -ne 0) { throw 'npm install failed' }
    }
    & $npm run build
    if ($LASTEXITCODE -ne 0) { throw 'frontend build failed' }
} finally {
    Pop-Location
}

Write-Host '==> Copy frontend/dist to backend static'
$staticDir = Join-Path $root 'backend\src\main\resources\static'
if (Test-Path $staticDir) { Remove-Item $staticDir -Recurse -Force }
New-Item -ItemType Directory -Path $staticDir -Force | Out-Null
Copy-Item (Join-Path $root 'frontend\dist\*') $staticDir -Recurse -Force

Write-Host '==> Maven package WAR'
& $mvn -f (Join-Path $root 'backend\pom.xml') clean package -Pwar -DskipTests
if ($LASTEXITCODE -ne 0) { throw 'maven war build failed' }

$sourceWar = Join-Path $root 'backend\target\archive-contract-system-1.0.0.war'
if (-not (Test-Path $sourceWar)) { throw "WAR not found: $sourceWar" }

$targetWar = Join-Path $root "backend\target\$warName.war"
if ($warName -ne 'archive-contract-system-1.0.0') {
    Copy-Item $sourceWar $targetWar -Force
}

$urlPath = if ($base -eq '/') { '/' } else { $base }
Write-Host ''
Write-Host "Build complete: $targetWar"
Write-Host "Tomcat context path: $urlPath"
Write-Host "Deploy: copy $warName.war to TOMCAT_HOME\webapps, then start Tomcat."
Write-Host "Access: http://server:8080$urlPath"
