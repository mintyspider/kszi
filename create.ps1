param(
    [string]$ProjectName
)

if (-not $ProjectName) {
    $ProjectName = Read-Host "Enter project name"
}

Write-Host "Creating project $ProjectName..." -ForegroundColor Green

$projectPath = Join-Path (Get-Location) $ProjectName
New-Item -ItemType Directory -Force -Path "$projectPath\src\main\kotlin" | Out-Null

# ========== Main.kt ==========
$mainCode = @"
fun main() {
    println("Project: $ProjectName")
    println("Kotlin is working!")
    println("Version: ${'$'}{KotlinVersion.CURRENT}")
}
"@
$mainCode | Out-File -FilePath "$projectPath\src\main\kotlin\Main.kt" -Encoding UTF8

# ========== build.ps1 ==========
$buildCode = @'
$jarName = "$ProjectName.jar"
kotlinc src\main\kotlin\Main.kt -include-runtime -d $jarName
if ($LASTEXITCODE -eq 0) {
    Write-Host "Built: $jarName" -ForegroundColor Green
} else {
    Write-Host "Compilation failed" -ForegroundColor Red
}
'@ -replace "ProjectName", $ProjectName
$buildCode | Out-File -FilePath "$projectPath\build.ps1" -Encoding ASCII

# ========== run.ps1 ==========
$runCode = @'
$jarName = "$ProjectName.jar"
if (Test-Path $jarName) {
    Write-Host "Running $jarName ..." -ForegroundColor Cyan
    java "-Dfile.encoding=UTF-8" -jar $jarName
} else {
    Write-Host "$jarName not found. Run .\build.ps1 first." -ForegroundColor Red
}
Read-Host "`nPress Enter to exit"
'@ -replace "ProjectName", $ProjectName
$runCode | Out-File -FilePath "$projectPath\run.ps1" -Encoding ASCII

# ========== README.md ==========
$readme = @"
# $ProjectName

Simple Kotlin CLI project.

REQUIREMENTS
- Kotlin (kotlinc)
- Java (JRE/JDK)

BUILD
.\build.ps1

RUN
.\run.ps1

STRUCTURE
- src\main\kotlin\Main.kt — entry point

OUTPUT
Creates a runnable JAR file.

---
Generated automatically.
"@
$readme | Out-File -FilePath "$projectPath\README.md" -Encoding UTF8

# ========== Итог ==========
Write-Host "Project created in: $projectPath" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Cyan
Write-Host "  cd $ProjectName"
Write-Host "  .\build.ps1"
Write-Host "  .\run.ps1"