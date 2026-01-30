@echo off
echo ============================================
echo Building Native Image for BIP32/BIP38 Generator
echo ============================================
echo.

echo Checking for GraalVM...
java -version 2>&1 | findstr /C:"GraalVM" >nul
if %errorlevel% neq 0 (
    echo ERROR: GraalVM not detected!
    echo Please ensure GraalVM is installed and JAVA_HOME points to GraalVM.
    echo Download from: https://www.graalvm.org/downloads/
    echo.
    pause
    exit /b 1
)

echo GraalVM detected!
echo.

echo Step 1: Building JAR...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)

echo.
echo Step 2: Building Native Image...
echo This may take 5-10 minutes...
echo.

call mvn -Pnative native:compile
if %errorlevel% neq 0 (
    echo ERROR: Native image build failed!
    pause
    exit /b 1
)

echo.
echo ============================================
echo Native Image Build Complete!
echo ============================================
echo.
echo Executable location: target\bip-generator.exe
echo.
echo To run: target\bip-generator.exe run com.bipgen.MainVerticle
echo.
pause
