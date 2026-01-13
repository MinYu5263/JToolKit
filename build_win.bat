@echo off
echo [Windows] Starting Build Process...

REM 1. 清理旧构建
if exist "dist" (
    echo Cleaning dist...
    rd /s /q "dist"
)

REM 2. 配置 JavaFX 路径 (优先读取系统环境变量，否则使用默认值)
REM 若系统未配置 PATH_TO_FX_MODS，将使用下方默认路径，请按需修改
if not defined PATH_TO_FX_MODS (
    set "PATH_TO_FX_MODS=D:/DevTools/JavaFX/javafx-jmods-21.0.9"
)

echo Using JavaFX mods: %PATH_TO_FX_MODS%

REM 3. 执行打包
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