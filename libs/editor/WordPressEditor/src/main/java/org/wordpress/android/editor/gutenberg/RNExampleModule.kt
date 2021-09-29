package org.wordpress.android.editor.gutenberg

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import org.wordpress.mobile.WPAndroidGlue.DeferredEventEmitter.JSEventEmitter

class RNExampleModule(
    private val mReactContext: ReactApplicationContext,
    private val mGutenbergBridgeJS2ParentWPCOM: GutenbergBridgeJS2ParentWPCOM
) : ReactContextBaseJavaModule(
        mReactContext
),
        JSEventEmitter {
    override fun getName(): String {
        return "RNExampleModule"
    }

    override fun emitToJS(eventName: String?, data: WritableMap?) {
        mReactContext.getJSModule(RCTDeviceEventEmitter::class.java).emit(
                eventName!!, data
        )
    }

    @ReactMethod fun justToast(text: String?) {
        mGutenbergBridgeJS2ParentWPCOM.justToast(text)
    }
}
