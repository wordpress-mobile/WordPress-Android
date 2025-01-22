package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

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
            // base the top offset on the system bar inset
            val topOffset = insets.getInsets(WindowInsets.Type.statusBars()).top

            // base the bottom offset on the navigation bar inset, but use a zero offset for the main activity
            // to accommodate the main BottomNavigationView
            val bottomOffset = if (this is WPMainActivity) {
                0
            } else {
                insets.getInsets(WindowInsets.Type.navigationBars()).bottom
            }

            // Adjust system bars padding to avoid overlap
            view.setPadding(
                0,
                topOffset,
                0,
                bottomOffset
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
}
