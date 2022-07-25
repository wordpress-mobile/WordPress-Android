package org.wordpress.android.ui.mysite.jetpackbadge

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.JetpackPoweredAnimatedImageBinding
import org.wordpress.android.databinding.JetpackPoweredCaptionBinding
import org.wordpress.android.databinding.JetpackPoweredTitleBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Caption
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Illustration
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredItem.Title
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.util.extensions.viewBinding

sealed class JetpackPoweredViewHolder<T : ViewBinding>(
    protected val binding: T
) : RecyclerView.ViewHolder(binding.root) {
    class IllustrationViewHolder(parentView: ViewGroup) :
            JetpackPoweredViewHolder<JetpackPoweredAnimatedImageBinding>(
                    parentView.viewBinding(
                            JetpackPoweredAnimatedImageBinding::inflate
                    )
            ) {
        @Suppress("unused")
        fun onBind(item: Illustration) = with(binding) {
            illustrationView.setAnimation(item.illustration)
        }
    }

    class TitleViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            JetpackPoweredViewHolder<JetpackPoweredTitleBinding>(
                    parentView.viewBinding(
                            JetpackPoweredTitleBinding::inflate
                    )
            ) {
        fun onBind(item: Title) = with(binding) {
            uiHelpers.setTextOrHide(title, item.text)
        }
    }

    class CaptionViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            JetpackPoweredViewHolder<JetpackPoweredCaptionBinding>(
                    parentView.viewBinding(JetpackPoweredCaptionBinding::inflate)
            ) {
        fun onBind(item: Caption) = with(binding) {
            uiHelpers.setTextOrHide(text, item.text)
        }
    }

    fun TextView.drawEmphasizedText(uiHelpers: UiHelpers, text: EmphasizedText) {
        val message = text.text
        if (text.emphasizeTextParams && message is UiStringResWithParams) {
            val params = message.params.map { uiHelpers.getTextOfUiString(this.context, it) as String }
            val textOfUiString = uiHelpers.getTextOfUiString(this.context, message)
            val spannable = SpannableString(textOfUiString)
            var index = 0
            for (param in params) {
                val indexOfParam = textOfUiString.indexOf(param, index)
                spannable.setSpan(
                        StyleSpan(Typeface.BOLD),
                        indexOfParam,
                        indexOfParam + param.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                index = textOfUiString.indexOf(param)
            }
            this.text = spannable
        } else {
            uiHelpers.setTextOrHide(this, message)
        }
    }
}
