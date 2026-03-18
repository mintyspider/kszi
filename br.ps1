# br.ps1 - build and run with auto recompile check
param(
    [string]$ProjectName
)

if (-not $ProjectName) {
    $ProjectName = Read-Host "Enter project name"
}

$projectPath = Join-Path (Get-Location) $ProjectName

if (-not (Test-Path $projectPath)) {
    Write-Host "ERROR: Project $ProjectName not found!" -ForegroundColor Red
    exit 1
}

Push-Location $projectPath

Write-Host "Working in project: $ProjectName" -ForegroundColor Green

$jarFile = "$ProjectName.jar"
$sourceFiles = Get-ChildItem -Path "src\main\kotlin" -Filter "*.kt" -Recurse

$needCompile = $false

if (-not (Test-Path $jarFile)) {
    Write-Host "JAR file not found. Compiling..." -ForegroundColor Yellow
    $needCompile = $true
}
else {
    $jarTime = (Get-Item $jarFile).LastWriteTime
    foreach ($file in $sourceFiles) {
        if ($file.LastWriteTime -gt $jarTime) {
            Write-Host "Source file $($file.Name) is newer. Recompiling..." -ForegroundColor Yellow
            $needCompile = $true
            break
        }
    }
}

if ($needCompile) {
    .\build.ps1
}

if ($LASTEXITCODE -eq 0) {
    Write-Host "Running..." -ForegroundColor Cyan
    .\run.ps1
}
else {
    Write-Host "Build failed. Aborting." -ForegroundColor Red
}

Pop-Location