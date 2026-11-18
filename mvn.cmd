@echo off
if not defined JAVA_HOME (
    for /f "delims=" %%I in ('where java 2^>nul') do (
        for %%J in ("%%~dpI..") do set "JAVA_HOME=%%~fJ"
        goto java_found
    )
)
:java_found
if not defined JAVA_HOME (
    if exist "C:\Program Files\JetBrains\IntelliJ IDEA 2026.3\jbr\bin\java.exe" (
        set "JAVA_HOME=C:\Program Files\JetBrains\IntelliJ IDEA 2026.3\jbr"
    )
)
if not defined JAVA_HOME (
    for /d %%J in ("C:\Program Files\Java\*") do if exist "%%J\bin\java.exe" set "JAVA_HOME=%%~fJ"
)
call "%~dp0mvnw.cmd" %*
