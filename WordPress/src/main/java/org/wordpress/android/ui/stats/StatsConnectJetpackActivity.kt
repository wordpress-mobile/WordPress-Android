package org.wordpress.android.ui.stats

import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import androidx.core.text.HtmlCompat
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
import org.wordpress.android.databinding.StatsJetpackConnectionActivityBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.FETCH_ACCOUNT
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.ui.JetpackConnectionSource.STATS
import org.wordpress.android.ui.JetpackConnectionWebViewActivity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.WPWebViewActivity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.WPUrlUtils
import org.wordpress.android.util.extensions.getSerializableExtraCompat
import javax.inject.Inject

/**
 * An activity that shows when user tries to open Stats without Jetpack connected.
 * It offers a link to the Jetpack connection flow.
 */
class StatsConnectJetpackActivity : LocaleAwareActivity() {
    private var mIsJetpackConnectStarted = false

    @Inject
    lateinit var mAccountStore: AccountStore

    @Inject
    lateinit var mDispatcher: Dispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initDagger()
        with(StatsJetpackConnectionActivityBinding.inflate(layoutInflater)) {
            setContentView(root)
            setActionBar()
            setTitle(string.stats)
            checkAndContinueJetpackConnectionFlow(savedInstanceState)
            initViews()
        }
    }

    private fun initDagger() {
        (application as WordPress).component().inject(this)
    }

    private fun StatsJetpackConnectionActivityBinding.setActionBar() {
        setSupportActionBar(toolbarLayout.toolbarMain)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(string.stats)
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * Continue Jetpack connect flow if coming from login/signup magic link.
     */
    private fun checkAndContinueJetpackConnectionFlow(savedInstanceState: Bundle?) {
        val continueJetpackConnect = intent.extras?.getBoolean(ARG_CONTINUE_JETPACK_CONNECT, false) ?: false
        if (savedInstanceState == null && continueJetpackConnect) {
            if (TextUtils.isEmpty(mAccountStore.account.userName)) {
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            } else {
                intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)?.let { startJetpackConnectionFlow(it) }
            }
        }
    }

    private fun StatsJetpackConnectionActivityBinding.initViews() {
        jetpackSetup.setOnClickListener {
            intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)?.let { site ->
                startJetpackConnectionFlow(site)
            }
        }
        jetpackFaq.setOnClickListener {
            WPWebViewActivity.openURL(this@StatsConnectJetpackActivity, FAQ_URL)
        }
        jetpackTermsAndConditions.text = HtmlCompat.fromHtml(
            String.format(resources.getString(string.jetpack_connection_terms_and_conditions), "<u>", "</u>"),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        jetpackTermsAndConditions.setOnClickListener {
            WPWebViewActivity.openURL(
                this@StatsConnectJetpackActivity,
                WPUrlUtils.buildTermsOfServiceUrl(this@StatsConnectJetpackActivity)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mDispatcher.register(this)
    }

    override fun onStop() {
        mDispatcher.unregister(this)
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun startJetpackConnectionFlow(siteModel: SiteModel) {
        mIsJetpackConnectStarted = true
        JetpackConnectionWebViewActivity
            .startJetpackConnectionFlow(this, STATS, siteModel, mAccountStore.hasAccessToken())
        finish()
    }

    @Subscribe(threadMode = MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!isFinishing) {
            if (event.isError) {
                val error = "${event.error.type} - ${event.error.message}"
                AppLog.e(API, "StatsConnectJetpackActivity.onAccountChanged error: $error")
            } else if (!mIsJetpackConnectStarted &&
                event.causeOfChange == FETCH_ACCOUNT &&
                !TextUtils.isEmpty(mAccountStore.account.userName)
            ) {
                intent.getSerializableExtraCompat<SiteModel>(WordPress.SITE)?.let { startJetpackConnectionFlow(it) }
            }
        }
    }

    companion object {
        const val ARG_CONTINUE_JETPACK_CONNECT = "ARG_CONTINUE_JETPACK_CONNECT"
        const val FAQ_URL = "https://wordpress.org/plugins/jetpack/#faq"
    }
}
