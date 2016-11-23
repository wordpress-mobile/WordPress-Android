import re
import sys
import os
import time

sys.path.append(os.path.join(os.environ['ANDROID_VIEW_CLIENT_HOME'], 'src'))
from com.dtmilano.android.viewclient import ViewClient, View
FLAG_ACTIVITY_NEW_TASK = 0x10000000

def init_view_client():
    package = 'org.wordpress.android'
    #activity = '.ui.posts.PostsActivity'
    activity = '.ui.accounts.NewBlogActivity'
    device, serialno = ViewClient.connectToDeviceOrExit()
    component = package + '/' + activity
    device.startActivity(component=component, flags=FLAG_ACTIVITY_NEW_TASK)
    ViewClient.sleep(2)
    return ViewClient(device, serialno), device

vc, device = init_view_client()
ViewClient.sleep(1)

def createABlog(name):
    vc.dump(0.1)
    settingsButton = vc.findViewWithText('Create a WordPress.com blog')
    while settingsButton == None:
        device.press('KEYCODE_DPAD_DOWN')
        device.press('KEYCODE_DPAD_DOWN')
        device.press('KEYCODE_DPAD_DOWN')
        device.press('KEYCODE_DPAD_DOWN')
        vc.dump(0.1)
        settingsButton = vc.findViewWithText('Create a WordPress.com blog')
    settingsButton.touch()
    vc.dump(0.5)
    device.type(name)
    time.sleep(1)
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_DPAD_DOWN')
    device.press('KEYCODE_DPAD_CENTER')
    time.sleep(30)

for i in xrange(90, 1001):
    createABlog("taliwuttalot" + str(i))
    time.sleep(2)
