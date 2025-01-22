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
        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && shouldAdjustContentForEdgeToEdge()) {
            adjustContentForEdgeToEdge()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun adjustContentForEdgeToEdge() {
        window.decorView.setOnApplyWindowInsetsListener { view, insets ->
            // set the system bars color
            val systemBarColor = getColorFromAttribute(com.google.android.material.R.attr.colorSurface)
            view.setBackgroundColor(systemBarColor)

            // Adjust system bars padding to avoid overlap
            view.setPadding(
                0,
                getTopOffset(insets),
                0,
                getBottomOffset(insets)
            )

            insets
        }
    }

    /**
     * Defaults to enforcing system bar padding for edge-to-edge on Android 15+ - descendants can override this and
     * return false for cases such as Compose-based activities where edge-to-edge is automatically supported,
     * or full-screen activities where we don't want to alter the window insets.
     */
    open fun shouldAdjustContentForEdgeToEdge() = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM)

    @RequiresApi(Build.VERSION_CODES.R)
    open fun getTopOffset(insets: WindowInsets) = insets.getInsets(WindowInsets.Type.statusBars()).top

    @RequiresApi(Build.VERSION_CODES.R)
    open fun getBottomOffset(insets: WindowInsets) = insets.getInsets(WindowInsets.Type.navigationBars()).bottom
}
