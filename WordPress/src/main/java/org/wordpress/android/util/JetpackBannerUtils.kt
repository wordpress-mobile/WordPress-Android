package org.wordpress.android.util

import android.view.View
import android.view.View.OnScrollChangeListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wordpress.android.R

object JetpackBannerUtils {
    fun showJetpackBannerIfScrolledToTop(banner: View, scrollableView: RecyclerView) {
        banner.isVisible = true

        val layoutManager = scrollableView.layoutManager as LinearLayoutManager
        val firstVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()
        val isEmpty = layoutManager.itemCount == 0

        banner.translationY = if (firstVisibleItemPosition == 0 || isEmpty) {
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
                val layoutManager = scrollableView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

                if (firstVisibleItemPosition == 0 && !isScrollAtTop) {
                    // Show the banner by moving up
                    isScrollAtTop = true
                    banner.animate().translationY(0f).start()
                } else if (firstVisibleItemPosition != 0 && isScrollAtTop) {
                    // Hide the banner by moving down
                    isScrollAtTop = false
                    val jetpackBannerHeight = banner.resources.getDimension(R.dimen.jetpack_banner_height)
                    banner.animate().translationY(jetpackBannerHeight).start()
                }
            }
        })
    }
}
