package org.wordpress.android.widgets

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

/**
 * MaterialTextView which enforces Western Arabic numerals.
 */
class MaterialTextViewWithNumerals @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {
    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text?.enforceWesternArabicNumerals(), type)
    }

    private fun CharSequence.enforceWesternArabicNumerals(): CharSequence {
        val textWithArabicNumerals = this
                // Replace Eastern Arabic numerals
                .replace(Regex("[٠-٩]")) { match -> (match.value.single() - '٠').toString() }
                // Replace Persian/Urdu numerals
                .replace(Regex("[۰-۹]")) { match -> (match.value.single() - '۰').toString() }

        val spannableText = SpannableString(textWithArabicNumerals)

        // Restore spans if text is an instance of Spanned
        if (this is Spanned) {
            TextUtils.copySpansFrom(this, 0, this.length, null, spannableText, 0)
        }

        return spannableText
    }
}
