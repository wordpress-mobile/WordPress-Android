#/bin/bash

device_registry="${HOME}/.android/devices.xml"
device_file=$1
device_handle=$(basename $device_file)

### Ensure that the device handle maps to a real device file
if [ ! -f "${device_file}" ]
then
    echo "ERROR: There is no file registered with that device handle at ${device_file}"
exit 1
fi

### If there's no pre-existing device registry, our job is easy
if [ ! -f $device_registry ]; then
    echo "The device registry doesn't exist – creating!"
    cp "${device_file}" "${device_registry}"
    echo "${device_handle} added to registry"

    exit 0
fi

$(dirname $0)/merge-hardware-profile-into-registry.py "${device_file}" "${device_registry}"
echo "${device_handle} added to registry"
