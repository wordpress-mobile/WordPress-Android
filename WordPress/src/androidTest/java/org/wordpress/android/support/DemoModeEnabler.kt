package org.wordpress.android.support

import android.os.ParcelFileDescriptor
import android.os.ParcelFileDescriptor.AutoCloseInputStream
import android.support.test.InstrumentationRegistry
import java.io.BufferedReader
import java.io.InputStreamReader

// Adapted From https://gist.github.com/hvisser/e716105f4e3cf2908ea463dbdb50679c with minor adjustments
class DemoModeEnabler {
    fun enable() {
        executeShellCommand("settings put global sysui_demo_allowed 1")
        sendCommand("exit")
        sendCommand("enter")

        // Notifications Off
        sendCommand("notifications", "visible" to "false")

        // Set up the wifi icon
        sendCommand("network", "wifi" to "show", "level" to "4")

        // Set up the cellular icon
        sendCommand("network", "mobile" to "show", "level" to "4")

        // Battery full, not plugged in
        sendCommand("battery", "level" to "100", "plugged" to "false")

        // 11:37 seems to be the most standard "Android time" (?)
        sendCommand("clock", "hhmm" to "1137")
    }

    fun disable() {
        sendCommand("exit")
    }

    private fun sendCommand(command: String, vararg extras: Pair<String, Any>) {
        val exec = StringBuilder("am broadcast -a com.android.systemui.demo -e command $command")
        for ((key, value) in extras) {
            exec.append(" -e $key $value")
        }
        executeShellCommand(exec.toString())
    }

    private fun executeShellCommand(command: String) {
        waitForCompletion(InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command))
    }

    private fun waitForCompletion(descriptor: ParcelFileDescriptor) {
        val reader = BufferedReader(InputStreamReader(AutoCloseInputStream(descriptor)))
        reader.use {
            it.readText()
        }
    }
}
