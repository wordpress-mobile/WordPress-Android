package org.wordpress.android.ui.sitecreation

import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.View
import android.view.ViewGroup

import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.ThemeStore

class NewSiteCreationCategoryFragment : NewSiteCreationBaseFormFragment<NewSiteCreationListener>() {
    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_category_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        // important for accessibility - talkback
        activity!!.setTitle(R.string.site_creation_category_title)
        rootView.findViewById<View>(R.id.site_creation_card_blog).setOnClickListener {
            if (mSiteCreationListener != null) {
                mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_BLOG)
            }
        }

        rootView.findViewById<View>(R.id.site_creation_card_website).setOnClickListener {
            if (mSiteCreationListener != null) {
                mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_WEBSITE)
            }
        }

        rootView.findViewById<View>(R.id.site_creation_card_portfolio).setOnClickListener {
            if (mSiteCreationListener != null) {
                mSiteCreationListener.withCategory(ThemeStore.MOBILE_FRIENDLY_CATEGORY_PORTFOLIO)
            }
        }
    }

    override fun onHelp() {
        if (mSiteCreationListener != null) {
            mSiteCreationListener.helpCategoryScreen()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (activity!!.application as WordPress).component().inject(this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_CREATION_CATEGORY_VIEWED)
        }
    }

    override fun onDetach() {
        super.onDetach()
        mSiteCreationListener = null
    }

    companion object {
        val TAG = "site_creation_category_fragment_tag"
    }
}
