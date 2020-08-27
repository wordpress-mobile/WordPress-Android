package org.wordpress.android.ui.accounts.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.login_intro_template_view.*
import org.wordpress.android.R

class LoginProloguePageFragment : Fragment() {
    @StringRes private var promoTitle: Int? = null
    @StringRes private var promoText: Int? = null
    @DrawableRes private var promoImage: Int? = null

    companion object {
        private const val KEY_PROMO_TITLE = "KEY_PROMO_TITLE"
        private const val KEY_PROMO_TEXT = "KEY_PROMO_TEXT"
        private const val KEY_PROMO_IMAGE = "KEY_PROMO_IMAGE"

        @JvmStatic
        fun newInstance(
            @StringRes promoTitle: Int,
            @StringRes promoText: Int,
            @DrawableRes promoImage: Int
        ) = LoginProloguePageFragment().apply {
            arguments = Bundle().apply {
                putInt(KEY_PROMO_TITLE, promoTitle)
                putInt(KEY_PROMO_TEXT, promoText)
                putInt(KEY_PROMO_IMAGE, promoImage)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            promoTitle = it.getInt(KEY_PROMO_TITLE)
            promoText = it.getInt(KEY_PROMO_TEXT)
            promoImage = it.getInt(KEY_PROMO_IMAGE)
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
        promoText?.let { promo_text.setText(it) }
        promoImage?.let { illustration_view.setImageResource(it) }
    }
}
