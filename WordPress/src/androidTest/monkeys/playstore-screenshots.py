#! /usr/bin/env python

import sys
import os
import time
import settings
from subprocess import Popen, PIPE
from com.dtmilano.android.viewclient import ViewClient

# App actions

def action_login(device, serialno):
    print("login")
    device.type(settings.username)
    device.press('KEYCODE_DPAD_DOWN')
    device.type(settings.password)
    device.press('KEYCODE_ENTER')
    # time to login
    time.sleep(10)
    # lose focus
    device.press('KEYCODE_DPAD_DOWN')
    time.sleep(1)

def action_open_my_sites(device, serialno):
    print("open my sites")
    device.press('KEYCODE_TAB')
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')
    # Wait for gravatars to load
    time.sleep(2)

def action_open_reader_freshlypressed(device, serialno):
    print("open reader")
    # Open the reader
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')
    device.press('KEYCODE_DPAD_RIGHT')
    device.press('KEYCODE_ENTER')
    # Wait for the reader to load articles / pictures
    time.sleep(5)

def action_open_notifications(device, serialno):
    print("open notifications tab")
    # Open the reader
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')
    for i in range(4):
        device.press('KEYCODE_DPAD_RIGHT')
    device.press('KEYCODE_ENTER')
    time.sleep(5)

def action_open_me(device, serialno):
    print("open me tab")
    # Open the reader
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')
    for i in range(2):
        device.press('KEYCODE_DPAD_RIGHT')
    device.press('KEYCODE_ENTER')
    time.sleep(5)

def action_open_editor_and_type_text(device, serialno):
    print("open editor")
    # Open My Sites
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')

    viewclient = ViewClient(device, serialno)
    viewclient.dump()
    view = viewclient.findViewById("org.wordpress.android:id/fab_button")
    view.touch()
    time.sleep(2)

    viewclient.dump()
    view = viewclient.findViewById("org.wordpress.android:id/post_content")

    # Type a sample text (spaces can't be entered via device.type())
    view.type(settings.example_post_content)
    time.sleep(2)

def action_open_stats(device, serialno):
    print("open stats tab")
    device.press('KEYCODE_TAB')
    for i in range(5):
        device.press('KEYCODE_DPAD_LEFT')
    viewclient = ViewClient(device, serialno)
    viewclient.dump()
    view = viewclient.findViewById("org.wordpress.android:id/row_stats")
    view.touch()
    time.sleep(5)

# Utilities

def lose_focus(serialno):
    # tap point 0,0  to lose focus
    _adb_shell(serialno, " input tap 0 0")
    time.sleep(1)

def take_screenshot(serialno, filename):
    os.popen("adb -s '%s' shell /system/bin/screencap -p /sdcard/screenshot.png" % (serialno))
    os.popen("adb -s '%s' pull /sdcard/screenshot.png '%s'" % (serialno, filename))

def launch_activity(device, package, activity):
    component = package + "/" + activity
    FLAG_ACTIVITY_NEW_TASK = 0x10000000
    device.startActivity(component=component, flags=FLAG_ACTIVITY_NEW_TASK)
    time.sleep(2)

def _adb_shell(serialno, command):
    print("adb -s '%s' shell \"%s\"" % (serialno, command))
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

def back(device):
    device.press('KEYCODE_BACK')

# Main scenario + screenshots

def run_tests_for_device_and_lang(device, serialno, filename, lang, packagename, apk):
    # Install the apk`
    reinstall_apk(serialno, packagename, apk)

    # Change language setting
    change_lang_settings(serialno, lang)

    # Launch the app
    launch_activity(device, packagename, "org.wordpress.android.ui.WPLaunchActivity")
    take_screenshot(serialno, lang + "-99-login-screen-" + filename)

    # Login into the app
    action_login(device, serialno)

    # Action!
    action_open_my_sites(device, serialno)
    take_screenshot(serialno, lang + "-1-my-sites-" + filename)
    action_open_reader_freshlypressed(device, serialno)
    take_screenshot(serialno, lang + "-2-reader-discover-" + filename)
    action_open_me(device, serialno)
    take_screenshot(serialno, lang + "-3-me-tab-" + filename)
    action_open_notifications(device, serialno)
    take_screenshot(serialno, lang + "-4-notifications-" + filename)
    action_open_stats(device, serialno)
    take_screenshot(serialno, lang + "-5-stats-" + filename)
    back(device)
    action_open_editor_and_type_text(device, serialno)
    take_screenshot(serialno, lang + "-6-editor-" + filename)
    # Close virtual keyboard
    back(device)
    # Close the editor
    back(device)

def list_devices():
    devices = []
    process = Popen("adb devices -l", stdout=PIPE, shell=True)
    for line in iter(process.stdout.readline, ''):
        split = line.split()
        if len(split) <= 1 or split[0] == "List":
            continue
        devices.append({"name": split[3].replace("model:", ""), "serialno": split[0]})
    process.communicate()
    return devices

def run_tests_on_device(packagename, apk, serialno, name, lang):
    device, serialno = ViewClient.connectToDeviceOrExit(verbose=False, serialno=serialno)
    filename = name + ".png"
    run_tests_for_device_and_lang(device, serialno, filename, lang, packagename, apk)

def run_tests_on_all_devices(packagename, apk, lang):
    devices = list_devices()
    if not devices:
        print("No device found")
        return
    for device in devices:
        print("Running on %s - language: %s" % (device, lang))
        run_tests_on_device(packagename, apk, device["serialno"], device["name"], lang)

def run_tests_on_all_devices_for_all_languages(packagename, apk):
    for lang in settings.languages:
        run_tests_on_all_devices(packagename, apk, lang)

def main():
    if len(sys.argv) < 3:
        sys.exit("usage: %s packagename apk" % sys.argv[0])
    packagename = sys.argv.pop(1)
    apk = sys.argv.pop(1)
    run_tests_on_all_devices_for_all_languages(packagename, apk)

main()
