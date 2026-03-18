$jarName = "$lab1.jar"
if (Test-Path $jarName) {
    Write-Host "???? Running $jarName ..." -ForegroundColor Cyan
    java "-Dfile.encoding=UTF-8" -jar $jarName
} else {
    Write-Host "??? $jarName not found. Run .\build.ps1 first." -ForegroundColor Red
}
Read-Host "`nPress Enter to exit"
