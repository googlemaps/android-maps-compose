#!/bin/bash
# generate_config_gif.sh — Configures SystemUI Demo Mode, records the Pixel 6 screen cycling through map configurations, and outputs an optimized high-quality loopable GIF.

set -e

OUTPUT_DIR="../docs/images"
mkdir -p "$OUTPUT_DIR"

# 1. Enable SystemUI Demo Mode for pixel-perfect clock status bars
./configure_screen.sh

echo "------------------------------------------------"
echo "Recording '2. Custom Configuration' cycle..."
echo "------------------------------------------------"
adb shell am force-stop com.google.maps.android.compose.snippets

# Start screenrecord targeting 7 seconds to capture the full 3-step cycle: 0 -> 1 -> 2 -> 0
adb shell screenrecord --time-limit 7 /sdcard/config_temp.mp4 &
RECORD_PID=$!

sleep 1 # Allow recorder buffer to stabilize

# Boot directly into Custom Configuration snippet
adb shell "am start -W -n com.google.maps.android.compose.snippets/com.google.maps.android.compose.snippets.MainActivity --es EXTRA_SNIPPET_TITLE \"2. Custom Configuration\""

sleep 7

echo "Pulling recording from device..."
adb pull /sdcard/config_temp.mp4 temp_config.mp4

echo "Converting to camera-trimmed, high-FPS loopable custom_config.gif..."
# Process via FFmpeg double-pass palette gen, trimming home screen buffers (start at 2.2s, length 6.2s)
ffmpeg -y -ss 00:00:02.2 -t 6.2 -i temp_config.mp4 -vf "fps=20,scale=360:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" -loop 0 "$OUTPUT_DIR/custom_config.gif"

# --- CLEANUP ---
echo "Cleaning up temporary files..."
adb shell rm -f /sdcard/config_temp.mp4
rm -f temp_config.mp4

# Restore normal device UI states
./configure_screen.sh off

echo "SUCCESS! Custom Configuration loop GIF written to docs/images/custom_config.gif!"
