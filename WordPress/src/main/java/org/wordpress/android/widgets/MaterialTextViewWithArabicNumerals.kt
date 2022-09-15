package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView

/**
 * MaterialTextView which enforces Western Arabic numerals.
 */
class MaterialTextViewWithArabicNumerals @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialTextView(context, attrs, defStyleAttr) {
    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text?.enforceWesternArabicNumerals(), type)
    }
}

fun CharSequence.enforceWesternArabicNumerals() = this
        // Replace Eastern Arabic numerals
        .replace(Regex("[٠-٩]")) { match -> (match.value.single() - '٠').toString() }
        // Replace Persian/Urdu numerals
        .replace(Regex("[۰-۹]")) { match -> (match.value.single() - '۰').toString() }
