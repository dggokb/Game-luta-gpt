param(
    [Parameter(Mandatory = $true)][string]$SourceRoot,
    [Parameter(Mandatory = $true)][string]$SourceBranch,
    [Parameter(Mandatory = $true)][string]$SourceSha
)

$ErrorActionPreference = 'Stop'

$source = (Resolve-Path $SourceRoot).Path
$required = @(
    'android/app/src/main/java/com/gamelutagpt/GameView.java',
    'android/app/src/main/res/drawable-nodpi',
    'tools/sprites/build_characters.py',
    'tools/sprites/tests'
)

foreach ($relative in $required) {
    $full = Join-Path $source $relative
    if (-not (Test-Path $full)) {
        throw "Fonte mobile incompativel com o motor PC. Ausente: $relative"
    }
}

Write-Host "=== Game Luta PC Export Engine ==="
Write-Host "Source branch: $SourceBranch"
Write-Host "Source SHA:    $SourceSha"
Write-Host "Source root:   $source"

if ($env:FULL_PC_EXPORT_VALIDATION -eq '1') {
    Write-Host "Full sprite validation enabled."
    Push-Location $source
    try {
        if (Test-Path 'tools/sprites/requirements.txt') {
            python -m pip install -r tools/sprites/requirements.txt
            if ($LASTEXITCODE -ne 0) { throw "Falha instalando dependencias de sprites." }
        }
        python tools/sprites/build_characters.py --write
        if ($LASTEXITCODE -ne 0) { throw "Falha gerando Character Packs." }
        python -m unittest discover -s tools/sprites/tests -v
        if ($LASTEXITCODE -ne 0) { throw "Testes de sprites falharam." }
        python tools/sprites/build_characters.py --check
        if ($LASTEXITCODE -ne 0) { throw "Validacao final dos Character Packs falhou." }
    }
    finally {
        Pop-Location
    }
} else {
    Write-Host "Skipping repeated sprite pipeline; using versioned generated outputs."
}

$env:GAME_LUTA_SOURCE_ROOT = $source

gradle -p pc clean jar --stacktrace
if ($LASTEXITCODE -ne 0) { throw "Compilacao desktop falhou." }

$packageRoot = 'pc/build/package'
$appDir = Join-Path $packageRoot 'GameLutaSpriteGPT'
$zipPath = 'pc/build/GameLutaSpriteGPT-Windows.zip'

if (Test-Path $packageRoot) { Remove-Item $packageRoot -Recurse -Force }
if (Test-Path $zipPath) { Remove-Item $zipPath -Force }
New-Item -ItemType Directory -Force -Path $packageRoot | Out-Null

jpackage --type app-image --name GameLutaSpriteGPT --input pc/build/libs --main-jar game-luta-sprite-pc.jar --main-class com.gamelutagpt.DesktopLauncher --dest $packageRoot
if ($LASTEXITCODE -ne 0) { throw "Empacotamento Windows com jpackage falhou." }

$engineSha = if ($env:GITHUB_SHA) { $env:GITHUB_SHA } else { 'local' }
$exportedAt = (Get-Date).ToUniversalTime().ToString('yyyy-MM-ddTHH:mm:ssZ')
$manifest = @"
Game Luta Sprite GPT - PC Export
Source branch: $SourceBranch
Source SHA: $SourceSha
Engine SHA: $engineSha
Exported at: $exportedAt
"@
Set-Content -Path (Join-Path $appDir 'EXPORT_INFO.txt') -Value $manifest -Encoding UTF8

Compress-Archive -Path $appDir -DestinationPath $zipPath -Force

if (-not (Test-Path $zipPath)) {
    throw "ZIP final nao foi criado."
}

Write-Host "Export concluido: $zipPath"
