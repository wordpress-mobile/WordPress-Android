@file:Suppress("MagicNumber")

package org.wordpress.android.ui.compose.theme.color

import android.annotation.SuppressLint
import androidx.compose.material.Colors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

@SuppressLint("ConflictingOnColor")
class WordPressColors : ThemeColors {
    private val blue30 = Color(color = 0xff399ce3)
    private val blue50 = Color(color = 0xff0675c4)
    private val blue70 = Color(color = 0xff044b7a)
    private val red50 = Color(color = 0xffd63638)
    private val red30 = Color(color = 0xfff86368)
    private val white = Color(color = 0xffffffff)
    private val black = Color(color = 0xff000000)
    private val darkGray = Color(color = 0xff121212)

    override fun light(): Colors = lightColors(
            primary = blue50,
            primaryVariant = blue70,
            secondary = blue50,
            secondaryVariant = blue70,
            background = white,
            surface = white,
            error = red50,
            onPrimary = white,
            onSecondary = white,
            onBackground = black,
            onSurface = black,
            onError = white
    )

    override fun dark(): Colors = darkColors(
            primary = blue30,
            primaryVariant = blue50,
            secondary = blue30,
            secondaryVariant = blue50,
            background = darkGray,
            surface = darkGray,
            error = red30,
            onPrimary = black,
            onSecondary = white,
            onBackground = white,
            onSurface = white,
            onError = black
    )
}
