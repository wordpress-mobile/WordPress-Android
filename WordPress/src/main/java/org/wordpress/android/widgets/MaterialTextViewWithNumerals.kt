package org.wordpress.android.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textview.MaterialTextView
import org.wordpress.android.util.extensions.enforceWesternArabicNumerals

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
}
