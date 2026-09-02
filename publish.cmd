@echo off
call "%~dp0deploy\publish-images.cmd" %*
exit /b %ERRORLEVEL%
