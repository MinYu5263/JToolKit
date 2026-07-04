winget install --id ImageMagick.ImageMagick -e#!/bin/bash
echo "[macOS] Starting Build Process..."

# 1. Clean previous build output
if [ -d "dist" ]; then
    echo "Cleaning dist..."
    rm -rf dist
fi

# 2. Configure JavaFX path, preferring PATH_TO_FX_MODS when present
DEFAULT_MODS="/Users/yourname/path/to/javafx-jmods-21"
FX_MODS="${PATH_TO_FX_MODS:-$DEFAULT_MODS}"

echo "Using JavaFX mods: $FX_MODS"

# 3. Run jpackage
echo "Running jpackage..."
jpackage @package-app-image.txt \
  --icon jtoolkit.icns \
  --module-path "$JAVA_HOME/jmods:$FX_MODS"

if [ $? -eq 0 ]; then
    echo "[SUCCESS] Build completed successfully!"
else
    echo "[ERROR] Build failed."
fi
