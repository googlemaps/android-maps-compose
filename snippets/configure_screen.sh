#!/bin/bash
# configure_screen.sh — Configures Android SystemUI Demo Mode for pixel-perfect, consistent screenshot top status bars.

if [ "$1" == "off" ]; then
    # SystemUI must be allowed to process the exit broadcast
    adb shell settings put global sysui_demo_allowed 1
    
    # Explicitly target the systemui package
    adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command exit
    
    # Clean up the settings
    adb shell settings put global sysui_tuner_demo_on 0
    adb shell settings put global sysui_demo_allowed 0
    
    echo "Screenshot mode disabled."
    exit 0
fi

# Wake up the device screen if asleep, and dismiss the lock screen keyguard
adb shell input keyevent KEYCODE_WAKE
adb shell wm dismiss-keyguard
sleep 0.5

# Enable Demo Mode controls
adb shell settings put global sysui_demo_allowed 1
adb shell settings put global sysui_tuner_demo_on 1

# Explicitly enter demo mode
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command enter

# Set time to 12:00
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command clock -e hhmm 1200

# Show full mobile data
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command network -e mobile show -e level 4 -e datatype false

# Hide notifications
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command notifications -e visible false

# Show full battery but not charging
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command battery -e plugged false -e level 100

# Hide Wi-Fi symbol
adb shell am broadcast -a com.android.systemui.demo -p com.android.systemui -e command network -e wifi hide

echo "Device configured for screenshots!"
