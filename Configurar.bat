@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Biblioteca.ps1" -Configurar
if errorlevel 1 pause
