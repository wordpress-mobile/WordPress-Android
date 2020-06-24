package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import kotlinx.android.synthetic.main.login_prologue_bottom_buttons_container_default.*
import kotlinx.android.synthetic.main.login_signup_screen.*
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.LOGIN_PROLOGUE_PAGED
import org.wordpress.android.analytics.AnalyticsTracker.Stat.LOGIN_PROLOGUE_VIEWED
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step.PROLOGUE
import javax.inject.Inject

class LoginPrologueFragment : Fragment() {
    private lateinit var loginPrologueListener: LoginPrologueListener

    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker

    companion object {
        const val TAG = "login_prologue_fragment_tag"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is LoginPrologueListener) {
            throw RuntimeException("$context must implement LoginPrologueListener")
        }
        loginPrologueListener = context
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.login_signup_screen, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nonNullActivity = checkNotNull(activity)
        (nonNullActivity.application as? WordPress)?.component()?.inject(this)

        if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
            bottom_buttons.removeAllViews()
            View.inflate(context, R.layout.login_prologue_bottom_buttons_container_unified, bottom_buttons)
        }

        first_button.setOnClickListener {
            unifiedLoginTracker.trackClick(Click.CONTINUE_WITH_WORDPRESS_COM)
            loginPrologueListener.showEmailLoginScreen()
        }

        second_button.setOnClickListener {
            if (BuildConfig.UNIFIED_LOGIN_AVAILABLE) {
                unifiedLoginTracker.trackClick(Click.LOGIN_WITH_SITE_ADDRESS)
                loginPrologueListener.loginViaSiteAddress()
            } else {
                loginPrologueListener.doStartSignup()
            }
        }

        val adapter = LoginProloguePagerAdapter(childFragmentManager)

        intros_pager.adapter = adapter
        intros_pager.addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                AnalyticsTracker.track(LOGIN_PROLOGUE_PAGED)
            }

            override fun onPageScrollStateChanged(state: Int) {}
        })

        if (adapter.count > 1) {
            // Using a TabLayout for simulating a page indicator strip
            tab_layout_indicator.setupWithViewPager(intros_pager, true)
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(LOGIN_PROLOGUE_VIEWED)
            unifiedLoginTracker.track(Flow.PROLOGUE, PROLOGUE)
        }
    }

    override fun onResume() {
        super.onResume()
        unifiedLoginTracker.setFlowAndStep(Flow.PROLOGUE, PROLOGUE)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // important for accessibility - talkback
        activity?.setTitle(R.string.login_prologue_screen_title)
    }
}
