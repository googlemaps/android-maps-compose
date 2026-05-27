#!/bin/bash
# generate_camera_gifs.sh — Automatically records and generates animated GIFs illustrating camera Move and Animate operations.

set -e

OUTPUT_DIR="../docs/images"
mkdir -p "$OUTPUT_DIR"

# 1. Enable SystemUI Demo Mode for standard professional clock/battery bars
./configure_screen.sh

echo "------------------------------------------------"
# --- RECORDING MOVE CAMERA ---
echo "Recording '1. Move Camera' transition..."
echo "------------------------------------------------"
adb shell am force-stop com.google.maps.android.compose.snippets

# Start screenrecord in the background targeting 5 seconds limit
adb shell screenrecord --time-limit 5 /sdcard/move_temp.mp4 &
RECORD_PID=$!

sleep 1 # Let screenrecorder warm up

# Boot directly into Move Camera snippet
adb shell "am start -W -n com.google.maps.android.compose.snippets/com.google.maps.android.compose.snippets.MainActivity --es EXTRA_SNIPPET_TITLE \"1. Move Camera\""

# Wait for the 5-second screen recording to safely complete on the device
sleep 5

echo "Pulling move camera recording..."
adb pull /sdcard/move_temp.mp4 temp_move.mp4

echo "Stitching and converting to camera_move.gif (360px width)..."
# Use ImageMagick convert to transcode the MP4 frames cleanly into an optimized loopable GIF
convert -delay 8 -loop 0 temp_move.mp4 -resize 360x "$OUTPUT_DIR/camera_move.gif"


echo "------------------------------------------------"
# --- RECORDING ANIMATE CAMERA ---
echo "Recording '2. Animate Camera' transition..."
echo "------------------------------------------------"
adb shell am force-stop com.google.maps.android.compose.snippets

# Start screenrecord in the background
adb shell screenrecord --time-limit 5 /sdcard/animate_temp.mp4 &
RECORD_PID=$!

sleep 1

# Boot directly into Animate Camera snippet
adb shell "am start -W -n com.google.maps.android.compose.snippets/com.google.maps.android.compose.snippets.MainActivity --es EXTRA_SNIPPET_TITLE \"2. Animate Camera\""

sleep 5

echo "Pulling animate camera recording..."
adb pull /sdcard/animate_temp.mp4 temp_animate.mp4

echo "Stitching and converting to camera_animate.gif (360px width)..."
convert -delay 8 -loop 0 temp_animate.mp4 -resize 360x "$OUTPUT_DIR/camera_animate.gif"


# --- CLEANUP ---
echo "Cleaning up temporary files on device and local workspace..."
adb shell rm -f /sdcard/move_temp.mp4 /sdcard/animate_temp.mp4
rm -f temp_move.mp4 temp_animate.mp4

# Restore normal device UI states
./configure_screen.sh off

echo "SUCCESS! Animated GIFs generated at docs/images/camera_move.gif and docs/images/camera_animate.gif!"
