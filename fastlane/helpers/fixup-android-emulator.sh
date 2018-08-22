#/bin/bash

device_key=$1

if [ -z "$device_key" ]
then
    echo "ERROR: You must provide the device name"
    exit 1
fi

### Set up the emulator without animations, per
### https://developer.android.com/training/testing/espresso/setup#set-up-environment

${ANDROID_HOME}/emulator/emulator -no-window -avd $device_key &

# Just in case this isn't on the user's $PATH
alias adb=${ANDROID_HOME}/platform-tools/adb

while [ "`adb wait-for-device shell getprop sys.boot_completed | tr -d '\r' `" != "1" ] ; do sleep 1; done

device_port=$(adb wait-for-device get-serialno)

adb -s $device_port shell settings put global development_settings_enabled 1
adb -s $device_port shell settings put global window_animation_scale 0.0
adb -s $device_port shell settings put global transition_animation_scale 0.0
adb -s $device_port shell settings put global animator_duration_scale 0.0

adb -s $device_port shell reboot -p
