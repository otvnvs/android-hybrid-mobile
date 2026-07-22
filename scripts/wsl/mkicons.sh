#!/bin/bash

ICONPATH=./icon.svg
MAINPATH=./app/src/main
RESPATH=$MAINPATH/res/

# Ensure all target resource directories exist before writing to them
mkdir -p "$RESPATH"/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}

echo "=== Processing Adaptive Foreground Layouts ==="
convert -background none $ICONPATH -resize 48x48   $RESPATH/mipmap-mdpi/ic_launcher_foreground.png
convert -background none $ICONPATH -resize 72x72   $RESPATH/mipmap-hdpi/ic_launcher_foreground.png
convert -background none $ICONPATH -resize 96x96   $RESPATH/mipmap-xhdpi/ic_launcher_foreground.png
convert -background none $ICONPATH -resize 144x144 $RESPATH/mipmap-xxhdpi/ic_launcher_foreground.png
convert -background none $ICONPATH -resize 192x192 $RESPATH/mipmap-xxxhdpi/ic_launcher_foreground.png

echo "=== Processing Themed Monochrome Layouts ==="
# Note: Added -fuzz 10% to cleanly catch any slight variations or gradients in your yellow fill
convert -background none -fuzz 10% -transparent "#ffff00" $ICONPATH -resize 48x48   $RESPATH/mipmap-mdpi/ic_launcher_monochrome.png
convert -background none -fuzz 10% -transparent "#ffff00" $ICONPATH -resize 72x72   $RESPATH/mipmap-hdpi/ic_launcher_monochrome.png
convert -background none -fuzz 10% -transparent "#ffff00" $ICONPATH -resize 96x96   $RESPATH/mipmap-xhdpi/ic_launcher_monochrome.png
convert -background none -fuzz 10% -transparent "#ffff00" $ICONPATH -resize 144x144 $RESPATH/mipmap-xxhdpi/ic_launcher_monochrome.png
convert -background none -fuzz 10% -transparent "#ffff00" $ICONPATH -resize 192x192 $RESPATH/mipmap-xxxhdpi/ic_launcher_monochrome.png

echo "=== Processing Web Application Master Files ==="
convert -background none $ICONPATH -resize 512x512 $MAINPATH/ic_launcher-web.png
convert -background none $ICONPATH -resize 512x512 ./appicon_512x512.png

echo "Success! Assets updated. Remember to run your clean build script."

