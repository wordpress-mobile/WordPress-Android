package org.wordpress.android.util

import android.content.res.Configuration
import android.view.View
import android.view.View.OnScrollChangeListener
import android.view.Window
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.config.JetpackPoweredFeatureConfig
import javax.inject.Inject

class JetpackBrandingUtils @Inject constructor(
    private val jetpackPoweredFeatureConfig: JetpackPoweredFeatureConfig,
    private val selectedSiteRepository: SelectedSiteRepository,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val buildConfigWrapper: BuildConfigWrapper
) {
    fun shouldShowJetpackBranding(): Boolean {
        val selectedSite = selectedSiteRepository.getSelectedSite()
        val isWpComSite = selectedSite != null && siteUtilsWrapper.isAccessedViaWPComRest(selectedSite)

        return isWpComSite && jetpackPoweredFeatureConfig.isEnabled() && !buildConfigWrapper.isJetpackApp
    }

    fun showJetpackBannerIfScrolledToTop(banner: View, scrollableView: RecyclerView) {
        banner.isVisible = true

        val layoutManager = scrollableView.layoutManager as LinearLayoutManager
        val isEmpty = layoutManager.itemCount == 0
        val scrollOffset = scrollableView.computeVerticalScrollOffset()

        banner.translationY = if (scrollOffset == 0 || isEmpty) {
            // Show
            0f
        } else {
            // Hide by moving down
            banner.resources.getDimension(R.dimen.jetpack_banner_height)
        }
    }

    fun initJetpackBannerAnimation(banner: View, scrollableView: RecyclerView) {
        scrollableView.setOnScrollChangeListener(object : OnScrollChangeListener {
            private var isScrollAtTop = true

            override fun onScrollChange(v: View?, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) {
                val scrollOffset = scrollableView.computeVerticalScrollOffset()

                if (scrollOffset == 0 && !isScrollAtTop) {
                    // Show the banner by moving up
                    isScrollAtTop = true
                    banner.animate().translationY(0f).start()
                } else if (scrollOffset != 0 && isScrollAtTop) {
                    // Hide the banner by moving down
                    isScrollAtTop = false
                    val jetpackBannerHeight = banner.resources.getDimension(R.dimen.jetpack_banner_height)
                    banner.animate().translationY(jetpackBannerHeight).start()
                }
            }
        })
    }

    /**
     * Sets the navigation bar color as same as Jetpack banner background color in portrait orientation.
     */
    fun setNavigationBarColorForBanner(window: Window) {
        if (window.context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            window.navigationBarColor = window.context.getColor(R.color.jetpack_banner_background)
        }
    }
}
