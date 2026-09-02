@echo off
setlocal
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish-images.ps1" %*
exit /b %ERRORLEVEL%
