@echo off
REM 绕过 PowerShell 执行策略，直接双击或 cmd 调用
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-lock-parallel.ps1" %*
