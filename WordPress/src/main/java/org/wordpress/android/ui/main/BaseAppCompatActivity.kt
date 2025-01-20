package org.wordpress.android.ui.main

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.util.extensions.getColorFromAttribute
import org.wordpress.android.util.extensions.setWindowStatusBarColor

open class BaseAppCompatActivity : AppCompatActivity() {
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val statusBarColor = getColorFromAttribute(com.google.android.material.R.attr.colorSurface)
            window.setWindowStatusBarColor(statusBarColor)
        }
    }
}
