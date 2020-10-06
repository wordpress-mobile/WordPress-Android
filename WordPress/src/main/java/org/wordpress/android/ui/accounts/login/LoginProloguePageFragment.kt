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

class LoginProloguePageFragment : Fragment() {
    @StringRes private var promoTitle: Int? = null
    @LayoutRes private var promoLayoutId: Int? = null
    @LayoutRes private var promoBackgroundId: Int? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.login_intro_template_view, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val inflater = LayoutInflater.from(view.context)

        promoTitle?.let { promo_title.setText(it) }
        promoLayoutId?.let {
            inflater.inflate(it, promo_layout_container, true)

            if (it == R.layout.login_prologue_second) {
                val editText = view.findViewById<EditText>(R.id.edit_text)
                editText.post {
                    editText.isPressed = true
                    editText.setSelection(editText.length())
                }
            }
        }
        promoBackgroundId?.let {
            inflater.inflate(it, promo_background_container, true)
        }
    }
}
