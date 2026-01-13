#!/bin/bash
echo "[macOS] Starting Build Process..."

# 1. 删除旧的 dist 目录
if [ -d "dist" ]; then
    echo "Cleaning old dist directory..."
    rm -rf dist
fi

# 2. 设置 JavaFX 路径 (请修改为你 Mac 上的实际路径)
# 建议写死或者 export 这个变量
PATH_TO_FX_MODS="/Users/yourname/path/to/javafx-jmods-21"

# 3. 执行 jpackage
# 注意：这里补充 Mac 特有的 icon (.icns) 和 module-path (使用冒号)
# 注意：Mac 下建议显式指定 java 路径，或者确保 $JAVA_HOME 已正确设置
echo "Running jpackage..."

jpackage @package-app-image.txt \
  --icon jtoolkit.icns \
  --module-path "$JAVA_HOME/jmods:$PATH_TO_FX_MODS"

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Build completed successfully!"
else
    echo "[ERROR] Build failed."
fi