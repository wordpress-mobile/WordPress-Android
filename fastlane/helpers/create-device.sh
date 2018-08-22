#/bin/bash

device_key=$1
device_id=$2
system_image=$3

if [ -z "${ANDROID_HOME}" ]
then
    echo 'ERROR: $ANDROID_HOME is not set as an environment variable.'

    if [ -d "${HOME}/Library/Android/sdk" ]; then
        printf "\n\nTry adding this to ${HOME}/.profile:\n"
        printf "export ANDROID_HOME=${HOME}/Library/Android/sdk\n\n"
    fi

    exit 1
fi

if [ -z "${device_key}" ]
then
    echo "ERROR: You must provide the device name (for example: my_new_device)"
    exit 2
fi

if [ -z "${device_id}" ]
then
    printf "\nERROR: You must provide the device ID (such as 5.1in WVGA). Use this command to get all device IDs:\n\n"
    echo '$ANDROID_HOME/tools/bin/avdmanager list'
    printf "\n"
    exit 3
fi

device_id_available=$($ANDROID_HOME/tools/bin/avdmanager list | grep "${device_id}")
if [ -z device_id_available ]
then
    printf "\nERROR: The device ID you've provided is not avaiable. Use this command to get all device IDs:\n\n"
    echo '$ANDROID_HOME/tools/bin/avdmanager list'
    printf "\n"
exit 4
fi

if [ -z "${system_image}" ]
then
    system_image="system-images;android-27;google_apis;x86"
fi

# Try to install the specified system image. If it's not already installed, it will be. If it *is*
# already installed, there's no error, so it doesn't hurt us to try.
$ANDROID_HOME/tools/bin/sdkmanager "${system_image}"

alias avd="$ANDROID_HOME/tools/bin/avdmanager"

avd --silent delete avd --name "${device_key}"
avd --silent create avd -n "${device_key}" -k "${system_image}" --device "${device_id}" --force
