@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.wordpress.android.ui.reader.models

import android.content.Context
import android.graphics.Color
import androidx.annotation.StyleRes
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.R
import org.wordpress.android.util.extensions.getColorFromAttributeOrRes
import java.util.Locale
import com.google.android.material.R as MaterialR

data class ReaderReadingPreferences @JvmOverloads constructor(
    val theme: Theme,
    val fontFamily: FontFamily = FontFamily.SANS_SERIF,
    val textSizeMultiplier: Float = 1.0f,
) {
    enum class Theme(
        @StyleRes val style: Int,
        val backgroundColorRes: Int,
        val baseTextColorRes: Int,
        val linkColorRes: Int,
    ) {
        PAPER(
            style = R.style.ReaderTheme_Paper,
            backgroundColorRes = R.color.reader_theme_paper_background,
            baseTextColorRes = R.color.reader_theme_paper_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        OLED(
            style = R.style.ReaderTheme_OLED,
            backgroundColorRes = R.color.reader_theme_oled_background,
            baseTextColorRes = R.color.reader_theme_oled_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SEPIA(
            style = R.style.ReaderTheme_Sepia,
            backgroundColorRes = R.color.reader_theme_sepia_background,
            baseTextColorRes = R.color.reader_theme_sepia_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SYSTEM(
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

        // TODO int color values (to be used on Views/Compose)

        init {
            val resources = context.resources
            val emphasisHigh = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_high_type)
            val emphasisMedium = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_medium)
            val emphasisLow = ResourcesCompat.getFloat(resources, R.dimen.emphasis_low)
            val emphasisDisabled = ResourcesCompat.getFloat(resources, MaterialR.dimen.material_emphasis_disabled)

            val backgroundColor = context.getColorFromAttributeOrRes(theme.backgroundColorRes)
            cssBackgroundColor = colorToHtmlColor(backgroundColor)

            val linkColor = context.getColorFromAttributeOrRes(theme.linkColorRes)
            cssLinkColor = colorToHtmlColor(linkColor)

            val baseTextColor = context.getColorFromAttributeOrRes(theme.baseTextColorRes)
            val baseTextColorR = Color.red(baseTextColor)
            val baseTextColorG = Color.green(baseTextColor)
            val baseTextColorB = Color.blue(baseTextColor)

            // same colors/emphasis as ReaderResourceVars class
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

    enum class FontFamily(val value: String) {
        SERIF("serif"),
        SANS_SERIF("sans-serif"),
        MONOSPACE("monospace"),
    }
}
