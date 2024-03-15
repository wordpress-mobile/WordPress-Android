@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.wordpress.android.ui.reader.models

import android.content.Context
import android.graphics.Color
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.R
import org.wordpress.android.util.extensions.getColorFromAttributeOrRes
import java.util.Locale
import com.google.android.material.R as MaterialR

data class ReaderReadingPreferences @JvmOverloads constructor(
    val theme: Theme = Theme.SYSTEM,
    val fontFamily: FontFamily = FontFamily.SANS,
    val fontSize: FontSize = FontSize.DEFAULT,
) {
    enum class Theme(
        @StringRes val displayNameRes: Int,
        @StyleRes val style: Int,
        val backgroundColorRes: Int,
        val baseTextColorRes: Int,
        val linkColorRes: Int,
    ) {
        PAPER(
            displayNameRes = R.string.reader_preferences_theme_paper,
            style = R.style.ReaderTheme_Paper,
            backgroundColorRes = R.color.reader_theme_paper_background,
            baseTextColorRes = R.color.reader_theme_paper_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        OLED(
            displayNameRes = R.string.reader_preferences_theme_oled,
            style = R.style.ReaderTheme_OLED,
            backgroundColorRes = R.color.reader_theme_oled_background,
            baseTextColorRes = R.color.reader_theme_oled_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SEPIA(
            displayNameRes = R.string.reader_preferences_theme_sepia,
            style = R.style.ReaderTheme_Sepia,
            backgroundColorRes = R.color.reader_theme_sepia_background,
            baseTextColorRes = R.color.reader_theme_sepia_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SYSTEM(
            displayNameRes = R.string.reader_preferences_theme_system,
            style = R.style.ReaderTheme_System,
            backgroundColorRes = MaterialR.attr.colorSurface,
            baseTextColorRes = MaterialR.attr.colorOnSurface,
            linkColorRes = R.color.reader_post_body_link,
        ),
    }

    class ThemeValues private constructor(
        context: Context,
        theme: Theme,
    ) {
        // CSS color values
        val cssBackgroundColor: String
        val cssTextColor: String
        val cssLinkColor: String
        val cssTextMediumColor: String
        val cssTextLightColor: String
        val cssTextExtraLightColor: String
        val cssTextDisabledColor: String

        // Int color values
        val intBackgroundColor: Int
        val intBaseTextColor: Int
        val intTextColor: Int
        val intLinkColor: Int

        init {
            val resources = context.resources
            val emphasisHigh = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_high_type)
            val emphasisMedium = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_medium)
            val emphasisLow = ResourcesCompat.getFloat(resources, R.dimen.emphasis_low)
            val emphasisDisabled = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_disabled)

            intBackgroundColor = context.getColorFromAttributeOrRes(theme.backgroundColorRes)
            cssBackgroundColor = colorToHtmlColor(intBackgroundColor)

            intLinkColor = context.getColorFromAttributeOrRes(theme.linkColorRes)
            cssLinkColor = colorToHtmlColor(intLinkColor)

            intBaseTextColor = context.getColorFromAttributeOrRes(theme.baseTextColorRes)
            val baseTextColorR = Color.red(intBaseTextColor)
            val baseTextColorG = Color.green(intBaseTextColor)
            val baseTextColorB = Color.blue(intBaseTextColor)

            // same colors/emphasis as ReaderResourceVars class
            intTextColor = Color.argb(
                (emphasisHigh * 255 + 0.5f).toInt(),
                baseTextColorR,
                baseTextColorG,
                baseTextColorB
            )
            cssTextColor = htmlRgbaColor(baseTextColorR, baseTextColorG, baseTextColorB, emphasisHigh)
            cssTextMediumColor = htmlRgbaColor(baseTextColorR, baseTextColorG, baseTextColorB, emphasisMedium)
            cssTextLightColor = htmlRgbaColor(baseTextColorR, baseTextColorG, baseTextColorB, emphasisDisabled)
            cssTextExtraLightColor = htmlRgbaColor(baseTextColorR, baseTextColorG, baseTextColorB, emphasisLow)
            cssTextDisabledColor = htmlRgbaColor(baseTextColorR, baseTextColorG, baseTextColorB, emphasisDisabled)
        }

        companion object {
            private const val HTML_RGBA_TEMPLATE = "rgba(%d, %d, %d, %.2f)"
            private const val HTML_RGB_MASK = 0xFFFFFF

            @JvmStatic
            fun from(context: Context, theme: Theme): ThemeValues {
                return ThemeValues(context, theme)
            }

            private fun colorToHtmlColor(color: Int): String {
                return String.format(Locale.US, "#%06X", HTML_RGB_MASK and color)
            }

            private fun htmlRgbaColor(red: Int, green: Int, blue: Int, alpha: Float): String {
                return HTML_RGBA_TEMPLATE.format(red, green, blue, alpha)
            }
        }
    }

    enum class FontFamily(
        @StringRes val displayNameRes: Int,
        val value: String,
    ) {
        SERIF(
            displayNameRes = R.string.reader_preferences_font_family_serif,
            value = "serif",
        ),
        SANS(
            displayNameRes = R.string.reader_preferences_font_family_sans,
            value = "sans-serif",
        ),
        MONO(
            displayNameRes = R.string.reader_preferences_font_family_mono,
            value = "monospace",
        ),
    }

    enum class FontSize(val value: Int) {
        EXTRA_SMALL(12),
        SMALL(14),
        NORMAL(16),
        LARGE(18),
        EXTRA_LARGE(20);

        val multiplier: Float
            get() = value / DEFAULT.value.toFloat()

        companion object {
            val DEFAULT = NORMAL
        }
    }
}
