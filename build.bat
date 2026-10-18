@echo off
java -version
if errorlevel 1 exit /b 1
if exist gradlew.bat (
  call gradlew.bat --no-daemon build
) else (
  gradle --no-daemon build
)
if errorlevel 1 exit /b 1
echo Build complete. Check build\libs for the Minemon jar.
