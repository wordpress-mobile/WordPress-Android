#/bin/bash

function replace_key_with (){
    key=$1
    value=$2
    path=$3

    awk "!/${key}/" $path > tmp
    mv tmp $path

    echo "${key}=${value}" >> $path
}

device_key=$1

if [ -z "$device_key" ]
then
    echo "ERROR: You must provide the device name"
    exit 1
fi

path="${HOME}/.android/avd/${device_key}.avd/config.ini"

if [ ! -f "$path" ]; then
    echo "ERROR: Unable to find the emulator at ${path} – are you sure your device name is '${device_key}'?"
    exit 2
fi

if [ -z "${ANDROID_HOME}" ]
then
    echo 'ERROR: $ANDROID_HOME is not set as an environment variable.'
    exit 3
fi

## Set up a disk partition size
replace_key_with 'disk.dataPartition.size' '800M' $path

## Don't allow fast boot
replace_key_with 'fastboot.forceColdBoot' 'yes' $path

# Set up the Hardware Profile
replace_key_with 'hw.gpu.enabled' 'yes' $path
replace_key_with 'hw.gpu.mode' 'auto' $path

# Ensure the Emulator works in Android Studio
replace_key_with 'skin.path' '_no_skin' $path


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
