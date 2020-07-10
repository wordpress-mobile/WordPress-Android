package org.wordpress.android.ui.accounts.login

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.wordpress.android.R

class LoginProloguePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val pages = listOf(
            Page(
                    R.string.login_promo_title_37_percent,
                    R.string.login_promo_text_unlock_the_power,
                    R.drawable.img_illustration_promo
            )
    )

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        return LoginProloguePageFragment.newInstance(page.promoTitle, page.promoText, page.promoImage)
    }

    override fun getCount(): Int {
        return pages.size
    }

    private data class Page(
        @StringRes val promoTitle: Int = 0,
        @StringRes val promoText: Int = 0,
        @DrawableRes val promoImage: Int = 0
    )
}
