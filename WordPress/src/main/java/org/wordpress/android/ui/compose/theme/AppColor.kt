package org.wordpress.android.ui.compose.theme

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Object containing static common colors used throughout the project. Note that the colors here are not SEMANTIC,
 * meaning they don't represent the usage of the color (e.g.: PrimaryButtonBackground) but instead they are raw
 * colors used throughout this app's design (e.g.: Green50).
 */
object AppColor {
    // Black & White
    @Stable
    val Black = Color(0xFF000000)

    @Stable
    val White = Color(0xFFFFFFFF)

    // Grays
    @Stable
    val DarkGray = Color(0xFF121212)

    @Stable
    val Gray40 = Color(0xFF787c82)

    @Stable
    val Gray50 = Color(0x0D000000)

    @Stable
    val Gray60 = Color(0x0DFFFFFF)

    @Stable
    val Gray70 = Color(0x1C1C1E)

    // Blues (Automattic Color Studio)
    @Stable
    val Blue30 = Color(0xFF399CE3)

    @Stable
    val Blue50 = Color(0xFF0675C4)

    @Stable
    val Blue70 = Color(0xFF044B7A)

    // Reds (Automattic Color Studio)
    @Stable
    val Red30 = Color(0xFFF86368)

    @Stable
    val Red50 = Color(0xFFD63638)

    // Greens (Automattic Color Studio)
    @Stable
    val Green50 = Color(0xFF008A20)

    // Jetpack Greens (Automattic Color Studio)
    @Stable
    val JetpackGreen40 = Color(0xFF069E08)

    @Stable
    val JetpackGreen50 = Color(0xFF008710)
}
