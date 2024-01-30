package org.wordpress.android.designsystem

import androidx.compose.material.Colors
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.compose.ui.graphics.Color

@Stable
class DesignSystemColorsBase(
    primaryForeground: Color,
    primaryBackground: Color,
    secondaryForeground: Color,
    secondaryBackground: Color,
    tertiaryForeground: Color,
    tertiaryBackground: Color,
    quartenaryBackground: Color,
    brandForeground: Color,
    brandBackground: Color,
    error: Color,
    warning: Color,
    wp: Color,
    wpBackground: Color,
    isLight: Boolean
) {
    var primaryForeground by mutableStateOf(primaryForeground, structuralEqualityPolicy())
        internal set
    var primaryBackground by mutableStateOf(primaryBackground, structuralEqualityPolicy())
        internal set
    var secondaryForeground by mutableStateOf(secondaryForeground, structuralEqualityPolicy())
        internal set
    var secondaryBackground by mutableStateOf(secondaryBackground, structuralEqualityPolicy())
        internal set
    var tertiaryForeground by mutableStateOf(tertiaryForeground, structuralEqualityPolicy())
        internal set
    var tertiaryBackground by mutableStateOf(tertiaryBackground, structuralEqualityPolicy())
        internal set
    var quartenaryBackground by mutableStateOf(quartenaryBackground, structuralEqualityPolicy())
        internal set
    var brandForeground by mutableStateOf(brandForeground, structuralEqualityPolicy())
        internal set
    var brandBackground by mutableStateOf(brandBackground, structuralEqualityPolicy())
        internal set
    var error by mutableStateOf(error, structuralEqualityPolicy())
        internal set
    var warning by mutableStateOf(warning, structuralEqualityPolicy())
        internal set
    var wp by mutableStateOf(wp, structuralEqualityPolicy())
        internal set
    var wpBackground by mutableStateOf(wpBackground, structuralEqualityPolicy())
        internal set
    var isLight by mutableStateOf(isLight, structuralEqualityPolicy())
        internal set

    /**
     * Returns a copy of this Colors, optionally overriding some of the values.
     */
    fun copy(
        primaryForeground: Color = this.primaryForeground,
        primaryBackground: Color = this.primaryBackground,
        secondaryForeground: Color = this.secondaryForeground,
        secondaryBackground: Color = this.secondaryBackground,
        tertiaryForeground: Color = this.tertiaryForeground,
        tertiaryBackground: Color = this.tertiaryBackground,
        quartenaryBackground: Color = this.quartenaryBackground,
        brandForeground: Color = this.brandForeground,
        brandBackground: Color = this.brandBackground,
        error: Color = this.error,
        warning: Color = this.warning,
        wp: Color = this.wp,
        wpBackground: Color = this.wpBackground,
        isLight: Boolean = this.isLight
    ): DesignSystemColorsBase = DesignSystemColorsBase(
        primaryForeground,
        primaryBackground,
        secondaryForeground,
        secondaryBackground,
        tertiaryForeground,
        tertiaryBackground,
        quartenaryBackground,
        brandForeground,
        brandBackground,
        error,
        warning,
        wp,
        wpBackground,
        isLight
    )

    override fun toString(): String {
        return "Colors(" +
                "primaryForeground=$primaryForeground, " +
                "primaryBackground=$primaryBackground, " +
                "secondaryForeground=$secondaryForeground, " +
                "secondaryBackground=$secondaryBackground, " +
                "tertiaryForeground=$tertiaryForeground, " +
                "tertiaryBackground=$tertiaryBackground, " +
                "quartenaryBackground=$quartenaryBackground, " +
                "brandForeground=$brandForeground, " +
                "brandBackground=$brandBackground, " +
                "error=$error, " +
                "warning=$warning, " +
                "wp=$wp, " +
                "wpBackground=$wpBackground, " +
                "isLight=$isLight" +
                ")"
    }
}

/**
 * Creates a complete color definition for the Design System light theme.
 *
 * @see darkColors
 */
fun lightColors(
    primaryForeground: Color = DesignSystemAppColor.Black,
    primaryBackground: Color = DesignSystemAppColor.White,
    secondaryForeground: Color = DesignSystemAppColor.GrayWIP2,
    secondaryBackground: Color = DesignSystemAppColor.Gray,
    tertiaryForeground: Color = DesignSystemAppColor.GrayWIP,
    tertiaryBackground: Color = DesignSystemAppColor.Gray10,
    quartenaryBackground: Color = DesignSystemAppColor.Gray30,
    brandForeground: Color = DesignSystemAppColor.Green,
    brandBackground: Color = DesignSystemAppColor.Green,
    error: Color = DesignSystemAppColor.Red,
    warning: Color = DesignSystemAppColor.Orange,
    wp: Color = DesignSystemAppColor.Blue,
    wpBackground: Color = DesignSystemAppColor.Blue
): DesignSystemColorsBase = DesignSystemColorsBase(
    primaryForeground,
    primaryBackground,
    secondaryForeground,
    secondaryBackground,
    tertiaryForeground,
    tertiaryBackground,
    quartenaryBackground,
    brandForeground,
    brandBackground,
    error,
    warning,
    wp,
    wpBackground,
    true
)

/**
 * Creates a complete color definition for the Design System dark theme.
 *
 * @see lightColors
 */
fun darkColors(
    primaryForeground: Color = DesignSystemAppColor.White,
    primaryBackground: Color = DesignSystemAppColor.Black,
    secondaryForeground: Color = DesignSystemAppColor.Gray20,
    secondaryBackground: Color = DesignSystemAppColor.Gray50,
    tertiaryForeground: Color = DesignSystemAppColor.Gray10,
    tertiaryBackground: Color = DesignSystemAppColor.Gray60,
    quartenaryBackground: Color = DesignSystemAppColor.Gray40,
    brandForeground: Color = DesignSystemAppColor.Green10,
    brandBackground: Color = DesignSystemAppColor.Green20,
    error: Color = DesignSystemAppColor.Red10,
    warning: Color = DesignSystemAppColor.Orange10,
    wp: Color = DesignSystemAppColor.Blue10,
    wpBackground: Color = DesignSystemAppColor.Blue20
): DesignSystemColorsBase = DesignSystemColorsBase(
    primaryForeground,
    primaryBackground,
    secondaryForeground,
    secondaryBackground,
    tertiaryForeground,
    tertiaryBackground,
    quartenaryBackground,
    brandForeground,
    brandBackground,
    error,
    warning,
    wp,
    wpBackground,
    false
)

internal fun DesignSystemColorsBase.updateColorsFrom(other: DesignSystemColorsBase) {
    primaryForeground = other.primaryForeground
    primaryBackground = other.primaryBackground
    secondaryForeground = other.secondaryForeground
    secondaryBackground = other.secondaryBackground
    tertiaryForeground = other.tertiaryForeground
    tertiaryBackground = other.tertiaryBackground
    quartenaryBackground = other.quartenaryBackground
    brandForeground = other.brandForeground
    error = other.error
    warning = other.warning
    wp = other.wp
    wpBackground = other.wpBackground
    isLight = other.isLight
}

/**
 * CompositionLocal used to pass [Colors] down the tree.
 */
internal val LocalColors = staticCompositionLocalOf { lightColors() }

