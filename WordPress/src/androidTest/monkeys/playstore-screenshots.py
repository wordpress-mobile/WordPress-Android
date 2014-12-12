#! /usr/bin/env python

import sys
import os
import time
import settings
from com.dtmilano.android.viewclient import ViewClient

# App actions

def action_login(device):
    device.type(settings.username)
    device.press('KEYCODE_DPAD_DOWN')
    device.type(settings.password)
    device.press('KEYCODE_ENTER')
    # time to login
    time.sleep(10)
    device.press('KEYCODE_DPAD_DOWN')

def action_open_drawer(device):
    for i in range(10):
        device.press('KEYCODE_DPAD_UP')
    device.press('KEYCODE_ENTER')
    # Wait for the animation to finish
    time.sleep(1)

# Utilities

def take_screenshot(serialno, filename):
    os.popen("adb -s '%s' shell /system/bin/screencap -p /sdcard/screenshot.png" % (serialno))
    os.popen("adb -s '%s' pull /sdcard/screenshot.png '%s'" % (serialno, filename))

def launch_activity(device, package, activity):
    component = package + "/" + activity
    FLAG_ACTIVITY_NEW_TASK = 0x10000000
    device.startActivity(component=component, flags=FLAG_ACTIVITY_NEW_TASK)
    time.sleep(2)

def _adb_shell(serialno, command):
    print "adb -s '%s' shell \"%s\"" % (serialno, command)
    os.popen("adb -s '%s' shell \"%s\"" % (serialno, command))

def change_lang_settings(serialno, lang):
    adb_command = "su -c 'setprop persist.sys.language %s; setprop persist.sys.country %s; stop; start'"
    _adb_shell(serialno, adb_command % (lang, lang))
    # time to reload
    time.sleep(15)
    unlock_screen(serialno)

def unlock_screen(serialno):
    _adb_shell(serialno, "input keyevent 82")
    time.sleep(1)

def reinstall_apk(serialno, packagename, apk):
    os.popen("adb -s '%s' uninstall '%s'" % (serialno, packagename))
    os.popen("adb -s '%s' install '%s'" % (serialno, apk))

# Main scenario + screenshots

def run_tests_for_device_and_lang(device, serialno, viewclient, filename, lang, packagename, apk):
    reinstall_apk(serialno, packagename, apk)
    change_lang_settings(serialno, lang)
    launch_activity(device, packagename, "org.wordpress.android.ui.WPLaunchActivity")
    take_screenshot(serialno, lang + "-login-screen-" + filename)
    action_login(device)
    take_screenshot(serialno, lang + "-post-login-" + filename)
    action_open_drawer(device)
    take_screenshot(serialno, lang + "-drawer-opened-" + filename)

def run_tests_for_device_and_lang2(device, serialno, viewclient, filename, lang, packagename, apk):
    take_screenshot(serialno, lang + "-login-screen-" + filename)
    action_login(device)
    take_screenshot(serialno, lang + "-post-login-" + filename)
    action_open_drawer(device)
    take_screenshot(serialno, lang + "-drawer-opened-" + filename)

def test1(device, serialno):
    viewclient = ViewClient(device, serialno)
    for i in range(10):
        device.press('KEYCODE_DPAD_UP')
    device.press('KEYCODE_ENTER')

def main():
    if len(sys.argv) < 5:
        sys.exit("usage: %s filename.png serialno packagename apk" % sys.argv[0])
    filename = sys.argv.pop(1)
    serialno = sys.argv.pop(1)
    packagename = sys.argv.pop(1)
    apk = sys.argv.pop(1)
    device, serialno = ViewClient.connectToDeviceOrExit(verbose=False, serialno=serialno)
    viewclient = ViewClient(device, serialno)
    run_tests_for_device_and_lang(device, serialno, viewclient, filename, "fr", packagename, apk)

main()
