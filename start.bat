@echo off
echo Building BIP32/BIP38 Generator...
call mvn clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! Starting server...
    echo.
    java -jar target\bip-generator-1.0.0-fat.jar
) else (
    echo.
    echo Build failed! Please check the errors above.
    pause
)
