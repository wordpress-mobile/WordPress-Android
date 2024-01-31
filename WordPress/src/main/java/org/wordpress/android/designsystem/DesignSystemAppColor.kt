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
    val Gray = Color(0xFFF2F2F7)

    @Stable
    val Gray10 = Color(0xFFC2C2C6)

    @Stable
    val Gray20 = Color(0x99EBEBF5)

    @Stable
    val Gray30 = Color(0xFF9B9B9E)

    @Stable
    val Gray40 = Color(0x993C3C43)

    @Stable
    val Gray50 = Color(0x4D3C3C43)

    @Stable
    val Gray60 = Color(0xFF4E4E4F)

    @Stable
    val Gray70 = Color(0xFF3A3A3C)

    @Stable
    val Gray80 = Color(0xFF2C2C2E)

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
