#!/bin/bash
# refresh_screenshots.sh — Automatically take screenshots for all or individual snippets on the Pontis device.

set -e

TARGET_SNIPPET="$1"
OUTPUT_DIR="../docs/images"
mkdir -p "$OUTPUT_DIR"

# Enable SystemUI Demo Mode for pixel-perfect and consistent clock, battery, and network states
./configure_screen.sh


# Declare associative array mapping snippet titles to filenames
declare -A FILENAMES
FILENAMES["1. Basic Map"]="basic_map.png"
FILENAMES["2. Custom Configuration"]="custom_config.png"
FILENAMES["1. Move Camera"]="camera_move.png"
FILENAMES["2. Animate Camera"]="camera_animate.png"
FILENAMES["3. Restrict Camera Bounds"]="camera_bounds.png"
FILENAMES["1. Basic Marker"]="marker_basic.png"
FILENAMES["2. Custom Marker Icon"]="marker_custom_icon.png"
FILENAMES["3. Marker Composable"]="marker_composable.png"
FILENAMES["4. Custom Info Window Composable"]="marker_info_window.png"
FILENAMES["1. Polyline"]="polyline.png"
FILENAMES["2. Polygon"]="polygon.png"
FILENAMES["3. Circle"]="circle.png"
FILENAMES["1. Marker Clustering"]="clustering.png"
FILENAMES["1. GeoJSON Layer"]="geojson_layer.png"
FILENAMES["2. KML Layer"]="kml_layer.png"
FILENAMES["1. Ground Overlay"]="ground_overlay.png"
FILENAMES["2. Tile Overlay"]="tile_overlay.png"
FILENAMES["3. WMS Tile Overlay"]="wms_tile_overlay.png"
FILENAMES["4. Compose Bitmap Descriptor"]="compose_bitmap_descriptor.png"
FILENAMES["5. Scale Bar Widget"]="scale_bar.png"

capture_snippet() {
  local title="$1"
  local filename="${FILENAMES[$title]}"
  
  if [ -z "$filename" ]; then
    # Fallback filename if not mapped explicitly
    filename=$(echo "$title" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9 ' | tr ' ' '_').png
  fi
  
  echo "------------------------------------------------"
  echo "Capturing: '$title' -> '$filename'..."
  echo "------------------------------------------------"
  
  # Force stop the application to guarantee a fresh boot and correct intent delivery
  adb shell am force-stop com.google.maps.android.compose.snippets
  sleep 1 # Allow OS to fully terminate the process asynchronously
  
  # Launch the app directly into that snippet with escaped quotes and wait-for-launch flag
  adb shell "am start -W -n com.google.maps.android.compose.snippets/com.google.maps.android.compose.snippets.MainActivity --es EXTRA_SNIPPET_TITLE \"$title\""
  
  # Wait for the map tiles and coordinates to fully render
  sleep 4
  
  # Capture screenshot
  adb shell screencap -p /sdcard/temp_snippet.png
  
  # Pull screenshot to output directory
  adb pull /sdcard/temp_snippet.png "$OUTPUT_DIR/$filename"
  
  # Scale down to 360px width for elegant markdown rendering and repository efficiency
  convert "$OUTPUT_DIR/$filename" -resize 360x "$OUTPUT_DIR/$filename"
  
  # Clean up on device
  adb shell rm /sdcard/temp_snippet.png
}

if [ -n "$TARGET_SNIPPET" ]; then
  # Capture a single snippet
  capture_snippet "$TARGET_SNIPPET"
else
  # Capture all snippets in order
  # Iterate key by key manually to guarantee ordering
  capture_snippet "1. Basic Map"
  capture_snippet "2. Custom Configuration"
  capture_snippet "1. Move Camera"
  capture_snippet "2. Animate Camera"
  capture_snippet "3. Restrict Camera Bounds"
  capture_snippet "1. Basic Marker"
  capture_snippet "2. Custom Marker Icon"
  capture_snippet "3. Marker Composable"
  capture_snippet "4. Custom Info Window Composable"
  capture_snippet "1. Polyline"
  capture_snippet "2. Polygon"
  capture_snippet "3. Circle"
  capture_snippet "1. Marker Clustering"
  capture_snippet "1. GeoJSON Layer"
  capture_snippet "2. KML Layer"
  capture_snippet "1. Ground Overlay"
  capture_snippet "2. Tile Overlay"
  capture_snippet "3. WMS Tile Overlay"
  capture_snippet "4. Compose Bitmap Descriptor"
  capture_snippet "5. Scale Bar Widget"
fi

# Restore the device's actual SystemUI states
./configure_screen.sh off

echo "Screenshots refresh complete! Images saved to snippets/$OUTPUT_DIR/"
