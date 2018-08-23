#!/usr/bin/python

import os, os.path, sys
import glob
from xml.etree import ElementTree

if len(sys.argv) < 3:
    print "You must provide at least two arguments to this script."
    print "merge-hardware-profile-into-registry.py ${path_to_hardware_profile.xml} ${path_to_hardware_profile_registry.xml}"

    sys.exit(1)

device_file = sys.argv[1]
registry = sys.argv[2]

ElementTree.register_namespace('d', "http://schemas.android.com/sdk/devices/3")
ElementTree.register_namespace('xsi', "http://www.w3.org/2001/XMLSchema-instance")

ns = {'d': 'http://schemas.android.com/sdk/devices/3',}

def device_exists_in_registry(device_name, registry_url):

    document = ElementTree.parse(registry_url).getroot()

    for device in document.findall('d:device', ns):

        this_device_name = device.find('d:name', ns).text

        if this_device_name == device_name:
            return True

    return None

def remove_device_from_registry(device_name, registry_url):

    document = ElementTree.parse(registry_url)

    element = None

    for device in document.getroot().findall('d:device', ns):
        this_device_name = device.find('d:name', ns).text

        if this_device_name == device_name:
            element = device

    # Once the loop ends, rewrite the document
    if element != None:
        document.getroot().remove(element)
        document.write(registry_url, encoding='utf-8', xml_declaration=True)


def add_device_to_registry(device_file_url, registry_url):
    device_document = ElementTree.parse(device_file_url)
    devices = device_document.getroot().findall('d:device', ns)

    if len(devices) == 0:
        return None

    device = devices[0]

    registry = ElementTree.parse(registry_url)
    registry.getroot().append(device)
    registry.write(registry_url, encoding='utf-8', xml_declaration=True)

def get_device_name_from_device_file(device_file_url):
    document = ElementTree.parse(device_file_url)
    devices = document.getroot().findall('d:device', ns)

    if len(devices) == 0:
        return None

    nameElement = devices[0].find('d:name', ns)

    if nameElement == None:
        return None;

    return nameElement.text


device_name = get_device_name_from_device_file(device_file)

if device_exists_in_registry(device_name, registry):
    print "Replacing", device_name
    remove_device_from_registry(device_name, registry)

add_device_to_registry(device_file, registry)
