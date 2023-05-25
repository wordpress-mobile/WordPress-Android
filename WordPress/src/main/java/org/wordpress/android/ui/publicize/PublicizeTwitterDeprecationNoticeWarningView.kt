package org.wordpress.android.ui.publicize

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import org.wordpress.android.R

class PublicizeTwitterDeprecationNoticeWarningView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.publicize_twitter_deprecation_notice_warning, this)
    }

    fun setTitle(text: String) {
        val title = findViewById<TextView>(R.id.publicize_twitter_deprecation_notice_header_title)
        title.text = text
    }

    fun setDescription(
        description: String,
        findOutMore: String,
        findOutMoreClick: () -> Unit,
    ) {
        val descriptionTextView = findViewById<TextView>(R.id.publicize_twitter_deprecation_notice_header_description)
        val space = " "
        val descriptionText: String = description + space
        val findOutMoreText: String = findOutMore
        val spannableTitle = SpannableString(descriptionText + findOutMoreText)
        val descriptionTextViewColor = descriptionTextView.currentTextColor
        spannableTitle.setSpan(
            ForegroundColorSpan(descriptionTextViewColor),
            0,
            descriptionText.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTitle.setSpan(
            ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.jetpack_green)),
            descriptionText.length,
            spannableTitle.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        spannableTitle.setSpan(
            object : ClickableSpan() {
                override fun onClick(view: View) {
                    findOutMoreClick()
                }
            },
            descriptionText.length,
            spannableTitle.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        descriptionTextView.movementMethod = LinkMovementMethod.getInstance()
        descriptionTextView.text = spannableTitle
    }
}
