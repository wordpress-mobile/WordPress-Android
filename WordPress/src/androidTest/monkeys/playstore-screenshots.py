#! /usr/bin/env python

import sys
import os
import time
import settings
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

def action_open_drawer(device, serialno):
    print("open drawer")
    for i in range(5):
        device.press('KEYCODE_DPAD_UP')
    device.press('KEYCODE_ENTER')
    # Wait for the animation to finish
    time.sleep(1)

# Menu drawer should be opened when calling this
def action_open_notifications(device, serialno):
    print("open notifications")
    for i in range(5):
        device.press('KEYCODE_DPAD_UP')
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_ENTER')
    # lose focus
    time.sleep(1)
    device.press('KEYCODE_DPAD_DOWN')
    # Wait for gravatars to load
    time.sleep(10)

# Menu drawer should be opened when calling this
def action_open_reader_posts_i_like(device, viewclient, serialno):
    print("open reader")
    # Open the reader
    for i in range(3):
        device.press('KEYCODE_DPAD_UP')
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_ENTER')
    time.sleep(1)
    # Be sure to focus the hamburger
    for i in range(3):
        device.press('KEYCODE_DPAD_UP')
        device.press('KEYCODE_DPAD_LEFT')
    # Select dropdown
    device.press('KEYCODE_DPAD_RIGHT')
    device.press('KEYCODE_ENTER')
    for i in range(3):
        device.press('KEYCODE_DPAD_UP')
    # Select Post I Like
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_ENTER')
    device.press('KEYCODE_SEARCH')
    # Wait for the reader to load articles / pictures
    time.sleep(10)
    lose_focus(serialno)

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

# Main scenario + screenshots

def run_tests_for_device_and_lang(device, serialno, viewclient, filename, lang, packagename, apk):
    reinstall_apk(serialno, packagename, apk)
    change_lang_settings(serialno, lang)
    launch_activity(device, packagename, "org.wordpress.android.ui.WPLaunchActivity")
    take_screenshot(serialno, lang + "-login-screen-" + filename)
    action_login(device, serialno)
    take_screenshot(serialno, lang + "-post-login-" + filename)
    action_open_drawer(device, serialno)
    take_screenshot(serialno, lang + "-drawer-opened-" + filename)
    action_open_notifications(device, serialno)
    take_screenshot(serialno, lang + "-notifications-" + filename)
    action_open_drawer(device, serialno)
    action_open_reader_posts_i_like(device, viewclient, serialno)
    take_screenshot(serialno, lang + "-reader-" + filename)

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
