package org.wordpress.android.ui.stats

import android.os.Bundle
import android.view.MenuItem
import androidx.core.text.HtmlCompat
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsJetpackConnectionActivityBinding
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.JetpackConnectionSource.STATS
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionActivity
import org.wordpress.android.ui.jetpackrestconnection.JetpackRestConnectionViewModel
import org.wordpress.android.ui.main.BaseAppCompatActivity
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject
import android.R as AndroidR

/**
 * An activity that shows when user tries to open Stats without Jetpack connected.
 * It offers a link to the Jetpack connection flow.
 */
class StatsConnectJetpackActivity : BaseAppCompatActivity() {
    @Inject
    lateinit var mAccountStore: AccountStore

    @Inject
    lateinit var mSelectedSiteRepository: SelectedSiteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDagger()
        with(StatsJetpackConnectionActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            initActionBar()
            setTitle(R.string.stats)
            initViews()
        }
    }

    private fun initDagger() {
        (application as WordPress).component().inject(this)
    }

    private fun StatsJetpackConnectionActivityBinding.initActionBar() {
        setSupportActionBar(toolbarLayout.toolbarMain)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(R.string.stats)
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun StatsJetpackConnectionActivityBinding.initViews() {
        jetpackSetup.setOnClickListener {
            startJetpackConnectionFlow(
                requireNotNull(intent.getSerializableExtraCompat(WordPress.SITE))
            )
        }
        jetpackFaq.setOnClickListener {
            WPWebViewActivity.openURL(this@StatsConnectJetpackActivity, FAQ_URL)
        }
        jetpackTermsAndConditions.text = HtmlCompat.fromHtml(
            String.format(resources.getString(R.string.jetpack_connection_terms_and_conditions), "<u>", "</u>"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        jetpackTermsAndConditions.setOnClickListener {
            WPWebViewActivity.openURL(
                this@StatsConnectJetpackActivity,
                WPUrlUtils.buildTermsOfServiceUrl(this@StatsConnectJetpackActivity)
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            AndroidR.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startJetpackConnectionFlow(siteModel: SiteModel) {
        if (!startJetpackRestConnectionFlow(siteModel)) {
            JetpackConnectionWebViewActivity
                .startJetpackConnectionFlow(this, STATS, siteModel, mAccountStore.hasAccessToken())
        }
        finish()
    }

    /**
     * If the Jetpack REST Connection flow is available and this site is able to use it, launch it and return true
     * so we skip the old WebView-based flow
     */
    private fun startJetpackRestConnectionFlow(site: SiteModel): Boolean {
        if (JetpackRestConnectionViewModel.canInitiateJetpackRestConnection(site)) {
            JetpackRestConnectionActivity.startJetpackRestConnectionFlow(
                this,
                JetpackRestConnectionViewModel.ConnectionSource.STATS
            )
            return true
        }
        return false
    }

    companion object {
        const val FAQ_URL = "https://wordpress.org/plugins/jetpack/#faq"
    }
}
