import re
import sys
import os
import time
import random
import shlex
import subprocess

sys.path.append(os.path.join(os.environ['ANDROID_VIEW_CLIENT_HOME'], 'src'))
from com.dtmilano.android.viewclient import ViewClient, View
FLAG_ACTIVITY_NEW_TASK = 0x10000000


class CMonkey:
    def __init__(self):
        self.package = 'org.wordpress.android'
        self.activity = '.ui.themes.ThemeBrowserActivity'
        self.component = self.package + '/' + self.activity
        self.init_device()
        
    def init_device(self):
        self.device, self.serialno = ViewClient.connectToDeviceOrExit()
        self.vc = ViewClient(self.device, self.serialno)

    def start_activity(self):
        self.device.startActivity(component=self.component,
                             flags=FLAG_ACTIVITY_NEW_TASK)

    def random_tap(self):
        x, y = random.randint(0, 2000), random.randint(0, 2000)
        self.device.touch(x, y)

    def test_comments(self):
        for i in range(20):
            self.random_tap()

def test():
    cm = CMonkey()
    for i in range(1000):
        cm.start_activity()
        args = shlex.split("adb shell monkey -p org.wordpress.android 500")
        p = subprocess.Popen(args)

test()