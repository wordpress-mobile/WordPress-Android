#/bin/bash

device_key=$1
device_id=$2

if [ -z "${ANDROID_HOME}" ]
then
    echo 'ERROR: $ANDROID_HOME is not set as an environment variable.'

    if [ -d "${HOME}/Library/Android/sdk" ]; then
        printf "\n\nTry adding this to ${HOME}/.profile:\n"
        printf "export ANDROID_HOME=${HOME}/Library/Android/sdk\n\n"
    fi

    exit 0
fi

if [ -z "${device_key}" ]
then
    echo "ERROR: You must provide the device name (for example: my_new_device)"
    exit 2
fi

if [ -z "${device_id}" ]
then
    printf "\nERROR: You must provide the device ID. Use this command to get all device IDs:\n\n"
    echo '$ANDROID_HOME/tools/bin/avdmanager list'
    printf "\n"
    exit 3
fi

$ANDROID_HOME/tools/bin/avdmanager --silent delete avd --name "${device_key}"
$ANDROID_HOME/tools/bin/avdmanager --silent create avd -n "${device_key}" -k "system-images;android-27;google_apis;x86" --device "${device_id}" --force
