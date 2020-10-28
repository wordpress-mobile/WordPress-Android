package org.wordpress.android.ui.accounts.login

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.FloatRange
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.login_intro_template_view.view.*
import kotlinx.android.synthetic.main.login_prologue_bottom_buttons_container_default.*
import kotlinx.android.synthetic.main.login_signup_screen.*
import org.wordpress.android.BuildConfig
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat.LOGIN_PROLOGUE_VIEWED
import org.wordpress.android.ui.accounts.UnifiedLoginTracker
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Click
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Flow
import org.wordpress.android.ui.accounts.UnifiedLoginTracker.Step.PROLOGUE
import org.wordpress.android.util.analytics.AnalyticsUtils
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

        (requireActivity().application as WordPress).component().inject(this)

        // setting up a full screen flags for the decor view of this fragment,
        // that will work with transparent status bar
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            val decorView: View = view
            var flags = decorView.systemUiVisibility
            flags = flags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            decorView.systemUiVisibility = flags
        }

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

        val adapter = LoginProloguePagerAdapter(this)

        intros_pager.adapter = adapter
        intros_pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                AnalyticsUtils.trackLoginProloguePages(position)
            }
        })
        intros_pager.setPageTransformer(object : ViewPager2.PageTransformer {
            override fun transformPage(page: View, position: Float) {
                // Since we want to achieve the illusion of having a single continuous background, we apply a
                // parallax effect to the foreground views of each page, making them enter and exit the screen
                // at a different speed than the background, which just follows the speed of the swipe gesture.
                page.apply {
                    applyParallaxEffect(promo_title, position, width, 0.25f)
                    applyParallaxEffect(promo_layout_container, position, width, 0.5f)
                }
            }

            /**
             * Apply a parallax effect to a given view.
             *
             * @param view The view to which the effect should be applied.
             * @param pagePosition Position of page relative to the current front-and-center position of the pager.
             *                     0 is front and center. 1 is one full page position to the right, and -1 is one
             *                     page position to the left.
             * @param pageWidth Total width of the page containing the view to which the effect should be applied.
             * @param speedFactor The factor by which the speed of the view is altered while the page is scrolled.
             *                    For example, a value of 0.25 would cause the view speed to increase by 25% when
             *                    moving out of the screen and to decrease by 25% when moving in.
             */
            private fun applyParallaxEffect(
                view: View,
                pagePosition: Float,
                pageWidth: Int,
                @FloatRange(from = 0.0, to = 1.0) speedFactor: Float
            ) {
                // If pagePosition is between -1 and 1, then at least a portion of the page is visible.
                if (pagePosition in -1.0..1.0) view.translationX = pagePosition * pageWidth * speedFactor
            }
        })

        if (adapter.itemCount > 1) {
            TabLayoutMediator(
                    tab_layout_indicator,
                    intros_pager,
                    TabLayoutMediator.TabConfigurationStrategy { _, _ -> }).attach()
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
