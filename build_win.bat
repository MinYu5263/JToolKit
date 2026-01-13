@echo off
echo [Windows] Starting Build Process...

REM 1. 删除旧的 dist 目录 (如果存在)
if exist "dist" (
    echo Cleaning old dist directory...
    rd /s /q "dist"
)

REM 2. 设置 JavaFX 路径 (请修改为你电脑上的实际路径，或者在系统环境变量中配置)
set PATH_TO_FX_MODS="D:/DevTools/JavaFX/javafx-jmods-21.0.9"

REM 3. 执行 jpackage
REM 
echo Running jpackage...
jpackage @package-app-image.txt ^
  --icon jtoolkit.ico ^
  --module-path "%JAVA_HOME%/jmods;%PATH_TO_FX_MODS%"

if %errorlevel% equ 0 (
    echo [SUCCESS] Build completed successfully!
) else (
    echo [ERROR] Build failed.
)

pause