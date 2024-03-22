package org.wordpress.android.designsystem

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color

/**
 * Object containing static common colors used throughout the project. Note that the colors here are not SEMANTIC,
 * meaning they don't represent the usage of the color (e.g.: PrimaryButtonBackground) but instead they are raw
 * colors used throughout this app's design (e.g.: Green50).
 */
object DesignSystemAppColor {
    // Black & White
    @Stable
    val Black = Color(0xFF000000)

    @Stable
    val White = Color(0xFFFFFFFF)

    // Grays
    @Stable
    val Gray = Color(0x66C2C2C6)

    @Stable
    val Gray22 = Color(0x38FFFFFF)

    @Stable
    val Gray30 = Color(0x4DFFFFFF)

    @Stable
    val Gray40 = Color(0x66FFFFFF)

    @Stable
    val Gray60 = Color(0x99EBEBF5)

    @Stable
    val DarkGray8 = Color(0x14000000)

    @Stable
    val DarkGray15 = Color(0x26000000)

    @Stable
    val DarkGray30 = Color(0x4D000000)

    @Stable
    val DarkGray40 = Color(0x66000000)

    @Stable
    val DarkGray55 = Color(0x8C000000)

    // Blues
    @Stable
    val Blue = Color(0xFF0675C4)

    @Stable
    val Blue10 = Color(0xFF399CE3)

    @Stable
    val Blue20 = Color(0xFF1689DB)

    // Greens
    @Stable
    val Green = Color(0xFF008710)

    @Stable
    val Green10 = Color(0xFF2FB41F)

    @Stable
    val Green20 = Color(0xFF069E08)

    // Reds
    @Stable
    val Red = Color(0xFFD63638)

    @Stable
    val Red10 = Color(0xFFE65054)

    // Oranges
    @Stable
    val Orange = Color(0xFFD67709)

    @Stable
    val Orange10 = Color(0xFFE68B28)
}
