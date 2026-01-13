#!/bin/bash
echo "[macOS] Starting Build Process..."

# 1. 清理旧构建
if [ -d "dist" ]; then
    echo "Cleaning dist..."
    rm -rf dist
fi

# 2. 配置 JavaFX 路径 (优先读取系统环境变量，否则使用默认值)
# 若系统未配置 PATH_TO_FX_MODS，将使用下方默认路径，请按需修改
DEFAULT_MODS="/Users/yourname/path/to/javafx-jmods-21"
FX_MODS="${PATH_TO_FX_MODS:-$DEFAULT_MODS}"

echo "Using JavaFX mods: $FX_MODS"

# 3. 执行打包
echo "Running jpackage..."
jpackage @package-app-image.txt \
  --icon jtoolkit.icns \
  --module-path "$JAVA_HOME/jmods:$FX_MODS"

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Build completed successfully!"
else
    echo "[ERROR] Build failed."
fi