package org.wordpress.android.ui.accounts.login

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import org.wordpress.android.R

class LoginProloguePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val pages = listOf(
            Page(
                    R.string.login_prologue_title_first,
                    R.layout.login_prologue_first
            ),
            Page(
                    R.string.login_prologue_title_second,
                    R.layout.login_prologue_second
            ),
            Page(
                    R.string.login_prologue_title_third,
                    R.layout.login_prologue_third
            ),
            Page(
                    R.string.login_prologue_title_fourth,
                    R.layout.login_prologue_fourth
            ),
            Page(
                    R.string.login_prologue_title_fifth,
                    R.layout.login_prologue_fifth
            )
    )

    override fun getItem(position: Int): Fragment {
        val page = pages[position]
        return LoginProloguePageFragment.newInstance(page.promoTitle, page.promoLayoutId)
    }

    override fun getCount(): Int {
        return pages.size
    }

    private data class Page(
        @StringRes val promoTitle: Int = 0,
        @LayoutRes val promoLayoutId: Int = 0
    )
}
