# путь к вашей папке bin от kotlinc
$env:KOTLIN_HOME = "D:\Kotlin\kotlinc\bin"  # ИЗМЕНИТЕ НА ВАШ ПУТЬ!

# Добавляем временно в PATH
$env:Path += ";$env:KOTLIN_HOME"

Write-Host "Kotlin is ready! Path: $env:KOTLIN_HOME" -ForegroundColor Green
Write-Host "Kotlin Version:" -ForegroundColor Yellow
kotlinc -version