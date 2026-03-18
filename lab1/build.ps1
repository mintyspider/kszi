$jarName = "$lab1.jar"
kotlinc src\main\kotlin\Main.kt -include-runtime -d $jarName
if ($LASTEXITCODE -eq 0) {
    Write-Host "??? Built: $jarName" -ForegroundColor Green
} else {
    Write-Host "??? Compilation failed" -ForegroundColor Red
}
