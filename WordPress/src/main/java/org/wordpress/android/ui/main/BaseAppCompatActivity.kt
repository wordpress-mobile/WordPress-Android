package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.util.extensions.getColorFromAttribute

/**
 * Base class for all activities - initially created to support Android 15's edge-to-edge, but can be extended
 * in the future to handle other cases
 */
open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && shouldEnforceEdgeToEdge()) {
            enforceEdgeToEdge()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun enforceEdgeToEdge() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            // set the system bars color
            val systemBarColor = getColorFromAttribute(com.google.android.material.R.attr.colorSurface)
            view.setBackgroundColor(systemBarColor)

            // Adjust system bars padding to avoid overlap
            val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
            view.setPadding(0, statusBarInsets.top, 0, 0)

            insets
        }
    }

    /**
     * Defaults to enforcing edge-to-edge on Android 15+, but descendants can override this to return
     * false for cases such as Compose-based activities where edge-to-edge is automatically supported
     */
    open fun shouldEnforceEdgeToEdge() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)
}
