#/bin/bash

device_config=$1

# Ensure the user provides a path argument
if [ -z "${device_config}" ]
then
    echo 'ERROR: You must provide a path to a device configuration'
exit 1
fi

# Ensure the device configuration exists
if [ ! -f "${device_config}" ]
then
    echo "ERROR: There is no emulator configuration file at ${device_config}"
exit 2
fi

device_config_filename=$(basename -- "$1")
device_handle="${device_config_filename%.*}"
emulator_base_folder=$(dirname $1)

# A handy function we'll need later
function join_by { local IFS="$1"; shift; echo "$*"; }
function system_image_for_device {
    device_file=$1

    line=$(cat "${device_file}" | grep "image.sysdir.1")    # Get the system image directory line
    directory="$(cut -d'=' -f2 <<<${line})"                 # Extract the system image directory

    IFS='/' read -r -a array <<< "${directory}"     # system-images android-27 google_apis x86
    echo ${array[1]}                                # android-27
}

# Set up some path variables
device_folder="${HOME}/.android/avd/${device_handle}.avd"
device_file="${device_folder}/config.ini"
config_file="${HOME}/.android/avd/${device_handle}.ini"

# Copy the files over to the AVD destination
# We need:
# - The config.ini file for the emulator
# - The .ini file that registers the emulator with AVD

rm -rf "${device_folder}"
mkdir "${device_folder}"
cp "${device_config}" "${device_file}"
cp "${emulator_base_folder}/base.ini" "${config_file}"

# Find the system image
system_image=$(system_image_for_device $device_file)

# Apply the device handle and system image to the configuration
sed -i '' "s/DEVICE_HANDLE/${device_handle}/g" "${config_file}"
sed -i '' "s/SYSTEM_IMAGE/${system_image}/g" "${config_file}"
