#!/usr/bin/env python

import sys
import xml.etree.ElementTree as ET
import xml.dom.minidom

def parse_inject_manifest(filename, versionCode, versionName):
    manifest = xml.dom.minidom.parse(filename)
    manifest.documentElement.setAttribute("android:versionCode", versionCode)
    manifest.documentElement.setAttribute("android:versionName", versionName)
    useSdk = manifest.createElement("uses-sdk")
    useSdk.setAttribute("android:minSdkVersion", "14")
    useSdk.setAttribute("android:targetSdkVersion", "23")
    manifest.documentElement.appendChild(useSdk)
    return manifest.toprettyxml("    ", "")

def get_version_from_build_gradle(filename):
    versionCode = ''
    versionName = ''
    for sline in (line.strip() for line in open('WordPress/build.gradle').readlines()):
        if sline.startswith("versionName"):
            versionName = sline.split()[1].replace('"', '')
        if sline.startswith("versionCode"):
            versionCode = sline.split()[1]
    return versionCode, versionName

def main():
    if len(sys.argv) != 3:
        print("Read versionCode and versionName in a build.gradle and inject it in a AndroidManifest.xml")
        print("Usage: %s AndroidManifest.xml build.gradle" % sys.argv[0])
        print("Example: %s AndroidManifest.xml build.gradle" % sys.argv[0])
        sys.exit(1)
    versionCode, versionName = get_version_from_build_gradle(sys.argv[2])
    print(parse_inject_manifest(sys.argv[1], versionCode, versionName))

if __name__ == "__main__":
    main()
