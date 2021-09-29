package org.wordpress.android.editor.gutenberg

import com.facebook.react.ReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.uimanager.ViewManager
import java.util.Arrays

class RNExamplePackage(private val mGutenbergBridgeJS2ParentWPCOM: GutenbergBridgeJS2ParentWPCOM) : ReactPackage {
    var exampleModule: RNExampleModule? = null
        private set

    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        exampleModule = RNExampleModule(reactContext, mGutenbergBridgeJS2ParentWPCOM)
        return Arrays.asList<NativeModule>(exampleModule)
    }

    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}

