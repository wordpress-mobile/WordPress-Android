package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.login_intro_template_view.*
import org.wordpress.android.R
import kotlin.math.min

class LoginProloguePageFragment : Fragment() {
    @StringRes private var promoTitle: Int? = null
    @LayoutRes private var promoLayoutId: Int? = null

    companion object {
        private const val KEY_PROMO_TITLE = "KEY_PROMO_TITLE"
        private const val KEY_PROMO_LAYOUT = "KEY_PROMO_LAYOUT"

        @JvmStatic
        fun newInstance(
            @StringRes promoTitle: Int,
            @LayoutRes promoLayoutId: Int
        ) = LoginProloguePageFragment().apply {
            arguments = Bundle().apply {
                putInt(KEY_PROMO_TITLE, promoTitle)
                putInt(KEY_PROMO_LAYOUT, promoLayoutId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            promoTitle = it.getInt(KEY_PROMO_TITLE)
            promoLayoutId = it.getInt(KEY_PROMO_LAYOUT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.login_intro_template_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        promoTitle?.let { promo_title.setText(it) }

        promo_layout_container.post {
            promoLayoutId?.let {
                val inflater = LayoutInflater.from(view.context)
                val content = inflater.inflate(promoLayoutId!!, promo_layout_container, false)

                val widthOfContainer = promo_layout_container.width
                val heightOfContainer = promo_layout_container.height

                val smallestDimensions = min(widthOfContainer, heightOfContainer).toFloat()
                val sizeOfContent = resources.getDimensionPixelOffset(R.dimen.login_prologue_content_area).toFloat()

                val scaleFactor = smallestDimensions / sizeOfContent

                content.scaleX = scaleFactor
                content.scaleY = scaleFactor

                promo_layout_container.addView(content)

                if (promoLayoutId == R.layout.login_prologue_second) {
                    val editText = view.findViewById<EditText>(R.id.edit_text)
                    editText.post {
                        editText.isPressed = true
                        editText.setSelection(editText.length())
                    }
                }
            }
        }
    }
}
