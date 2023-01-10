package org.wordpress.android.ui.accounts.login

import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import org.wordpress.android.R

class LoginProloguePagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    private val pages = listOf(
        Page(
            R.string.login_prologue_title_first,
            R.layout.login_prologue_first,
            R.layout.login_prologue_background_first
        ),
        Page(
            R.string.login_prologue_title_second,
            R.layout.login_prologue_second,
            R.layout.login_prologue_background_second
        ),
        Page(
            R.string.login_prologue_title_third,
            R.layout.login_prologue_third,
            R.layout.login_prologue_background_third
        ),
        Page(
            R.string.login_prologue_title_fourth,
            R.layout.login_prologue_fourth,
            R.layout.login_prologue_background_fourth
        ),
        Page(
            R.string.login_prologue_title_fifth,
            R.layout.login_prologue_fifth,
            R.layout.login_prologue_background_fifth
        )
    )

    private data class Page(
        @StringRes val promoTitle: Int = 0,
        @LayoutRes val promoLayoutId: Int = 0,
        @LayoutRes val promoBackgroundId: Int = 0
    )

    override fun getItemCount(): Int {
        return pages.size
    }

    override fun createFragment(position: Int): Fragment {
        val page = pages[position]
        return LoginProloguePageFragment.newInstance(page.promoTitle, page.promoLayoutId, page.promoBackgroundId)
    }
}
