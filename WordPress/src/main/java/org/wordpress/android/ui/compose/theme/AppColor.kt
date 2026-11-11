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
    val DarkGray90 = Color(0xE6121212)

    @Stable
    val Gray0 = Color(0xFFF6F7F7)

    // Grays tagged "interpolated" below sit evenly between adjacent Color Studio stops. They exist only to fill
    // the 5-step M3 surfaceContainer ramp, which the published ramp is too coarse to cover on its own.
    @Stable
    val Gray2 = Color(0xFFEDEEEF) // interpolated

    @Stable
    val Gray3 = Color(0xFFE5E5E6) // interpolated

    @Stable
    val Gray5 = Color(0xFFDCDCDE)

    @Stable
    val Gray10 = Color(0xFFC3C4C7)

    @Stable
    val Gray30 = Color(0xFF8c8f94)

    @Stable
    val Gray40 = Color(0xFF787c82)

    @Stable
    val Gray50 = Color(0xFF646970)

    @Stable
    val Gray60 = Color(0xFF50575E)

    @Stable
    val Gray70 = Color(0xFF3C434A)

    @Stable
    val Gray80 = Color(0xFF2C3338)

    @Stable
    val Gray85 = Color(0xFF252B30) // interpolated

    @Stable
    val Gray90 = Color(0xFF1D2327)

    @Stable
    val Gray95 = Color(0xFF171C1F) // interpolated

    @Stable
    val Gray100 = Color(0xFF101517)

    // Blues (Automattic Color Studio)
    @Stable
    val Blue5 = Color(0xFFBBE0FA)

    @Stable
    val Blue30 = Color(0xFF399CE3)

    @Stable
    val Blue50 = Color(0xFF0675C4)

    @Stable
    val Blue70 = Color(0xFF044B7A)

    @Stable
    val Blue80 = Color(0xFF02395C)

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
    val JetpackGreen5 = Color(0xFFD0E6B8)

    @Stable
    val JetpackGreen30 = Color(0xFF2FB41F)

    @Stable
    val JetpackGreen50 = Color(0xFF008710)

    @Stable
    val JetpackGreen70 = Color(0xFF005B18)

    @Stable
    val JetpackGreen90 = Color(0xFF003010)

    // Yellows
    @Stable
    val Yellow50 = Color(0xFF9D6E00)

    // Oranges
    @Stable
    val Orange40 = Color(0xFFD67709)

    @Stable
    val Orange50 = Color(0xFFB26200)
}
