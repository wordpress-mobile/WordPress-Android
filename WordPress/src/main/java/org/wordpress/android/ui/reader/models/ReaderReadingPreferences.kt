@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package org.wordpress.android.ui.reader.models

import android.content.Context
import android.graphics.Color
import androidx.core.content.res.ResourcesCompat
import org.wordpress.android.R
import org.wordpress.android.util.HtmlUtils
import org.wordpress.android.util.extensions.getColorFromAttributeOrRes
import com.google.android.material.R as MaterialR

data class ReaderReadingPreferences(
    val theme: Theme,
    val fontFamily: FontFamily = FontFamily.SANS_SERIF,
    val textSizeMultiplier: Float = 1.0f,
) {
    enum class Theme(
        val backgroundColorRes: Int,
        val baseTextColorRes: Int,
        val linkColorRes: Int,
    ) {
        PAPER(
            backgroundColorRes = R.color.reader_theme_paper_background,
            baseTextColorRes = R.color.reader_theme_paper_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        OLED(
            backgroundColorRes = R.color.reader_theme_oled_background,
            baseTextColorRes = R.color.reader_theme_oled_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SEPIA(
            backgroundColorRes = R.color.reader_theme_sepia_background,
            baseTextColorRes = R.color.reader_theme_sepia_text,
            linkColorRes = R.color.reader_post_body_link,
        ),
        SYSTEM(
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

            cssBackgroundColor = HtmlUtils.colorResToHtmlColor(context, theme.backgroundColorRes)
            cssLinkColor = HtmlUtils.colorResToHtmlColor(context, theme.linkColorRes)

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

            fun from(context: Context, theme: Theme): ThemeValues {
                return ThemeValues(context, theme)
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
