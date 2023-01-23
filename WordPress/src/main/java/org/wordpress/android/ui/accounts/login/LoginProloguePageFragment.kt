package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import org.wordpress.android.R
import org.wordpress.android.databinding.LoginIntroTemplateViewBinding
import org.wordpress.android.util.ActivityUtils
import kotlin.math.min

class LoginProloguePageFragment : Fragment(R.layout.login_intro_template_view) {
    @StringRes
    private var promoTitle: Int? = null

    @LayoutRes
    private var promoLayoutId: Int? = null

    @LayoutRes
    private var promoBackgroundId: Int? = null

    companion object {
        private const val KEY_PROMO_TITLE = "KEY_PROMO_TITLE"
        private const val KEY_PROMO_LAYOUT = "KEY_PROMO_LAYOUT"
        private const val KEY_PROMO_BACKGROUND = "KEY_PROMO_BACKGROUND"

        @JvmStatic
        fun newInstance(
            @StringRes promoTitle: Int,
            @LayoutRes promoLayoutId: Int,
            @LayoutRes promoBackgroundId: Int
        ) = LoginProloguePageFragment().apply {
            arguments = Bundle().apply {
                putInt(KEY_PROMO_TITLE, promoTitle)
                putInt(KEY_PROMO_LAYOUT, promoLayoutId)
                putInt(KEY_PROMO_BACKGROUND, promoBackgroundId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            promoTitle = it.getInt(KEY_PROMO_TITLE)
            promoLayoutId = it.getInt(KEY_PROMO_LAYOUT)
            promoBackgroundId = it.getInt(KEY_PROMO_BACKGROUND)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val inflater = LayoutInflater.from(view.context)
        val binding = LoginIntroTemplateViewBinding.bind(view)

        promoTitle?.let { binding.promoTitle.setText(it) }

        val container = binding.promoLayoutContainer
        container.post {
            promoLayoutId?.let {
                val content = inflater.inflate(promoLayoutId!!, container, false)

                val widthOfContainer = container.width
                val heightOfContainer = container.height

                val smallestDimension = min(widthOfContainer, heightOfContainer).toFloat()
                val sizeOfContent = resources.getDimensionPixelOffset(R.dimen.login_prologue_content_area).toFloat()

                val scaleFactor = smallestDimension / sizeOfContent

                content.scaleX = scaleFactor
                content.scaleY = scaleFactor

                container.addView(content)

                // add a cursor to the end of the EditText at second prologue screen
                if (promoLayoutId == R.layout.login_prologue_second) {
                    val editText = view.findViewById<EditText>(R.id.edit_text)
                    editText.post {
                        editText.isPressed = true
                        editText.setSelection(editText.length())
                    }
                }

                // format text from HTML to show bold on third prologue screen
                if (promoLayoutId == R.layout.login_prologue_third) {
                    view.findViewById<TextView>(R.id.text_one).text = HtmlCompat.fromHtml(
                        getString(R.string.login_prologue_third_subtitle_one),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    view.findViewById<TextView>(R.id.text_two).text = HtmlCompat.fromHtml(
                        getString(R.string.login_prologue_third_subtitle_two),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                    view.findViewById<TextView>(R.id.text_three).text = HtmlCompat.fromHtml(
                        getString(R.string.login_prologue_third_subtitle_three),
                        HtmlCompat.FROM_HTML_MODE_LEGACY
                    )
                }
            }
        }
        promoBackgroundId?.let {
            inflater.inflate(it, binding.promoBackgroundContainer, true)
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.let { ActivityUtils.hideKeyboard(it) }
    }
}
