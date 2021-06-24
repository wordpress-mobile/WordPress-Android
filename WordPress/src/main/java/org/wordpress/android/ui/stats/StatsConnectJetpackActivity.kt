package org.wordpress.android.ui.stats

import android.os.Bundle
import android.text.Html
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R.id
import org.wordpress.android.R.layout
import org.wordpress.android.R.string
import org.wordpress.android.WordPress
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
import org.wordpress.android.ui.stats.StatsConnectJetpackActivity
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.WPUrlUtils
import javax.inject.Inject

/**
 * An activity that shows when user tries to open Stats without Jetpack connected.
 * It offers a link to the Jetpack connection flow.
 */
class StatsConnectJetpackActivity : LocaleAwareActivity() {
    private var mIsJetpackConnectStarted = false

    @JvmField @Inject
    var mAccountStore: AccountStore? = null

    @JvmField @Inject
    var mDispatcher: Dispatcher? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)
        setContentView(layout.stats_jetpack_connection_activity)
        val toolbar = findViewById<Toolbar>(id.toolbar_main)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setTitle(string.stats)
            actionBar.setDisplayShowTitleEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
        }
        setTitle(string.stats)

        // Continue Jetpack connect flow if coming from login/signup magic link.
        if (savedInstanceState == null && intent != null && intent.extras != null && intent.extras!!
                        .getBoolean(ARG_CONTINUE_JETPACK_CONNECT, false)) {
            if (TextUtils.isEmpty(mAccountStore!!.account.userName)) {
                mDispatcher!!.dispatch(AccountActionBuilder.newFetchAccountAction())
            } else {
                startJetpackConnectionFlow(intent.getSerializableExtra(WordPress.SITE) as SiteModel)
            }
        }
        val setupButton = findViewById<Button>(id.jetpack_setup)
        setupButton.setOnClickListener { v: View? ->
            startJetpackConnectionFlow(
                    this@StatsConnectJetpackActivity.intent.getSerializableExtra(WordPress.SITE) as SiteModel
            )
        }
        val jetpackFaq = findViewById<Button>(id.jetpack_faq)
        jetpackFaq.setOnClickListener { v: View? ->
            WPWebViewActivity.openURL(
                    this@StatsConnectJetpackActivity,
                    FAQ_URL
            )
        }
        val jetpackTermsAndConditions = findViewById<TextView>(id.jetpack_terms_and_conditions)
        jetpackTermsAndConditions.text = Html.fromHtml(
                String.format(
                        resources.getString(string.jetpack_connection_terms_and_conditions), "<u>", "</u>"
                )
        )
        jetpackTermsAndConditions.setOnClickListener { v: View? ->
            WPWebViewActivity.openURL(
                    this@StatsConnectJetpackActivity,
                    WPUrlUtils.buildTermsOfServiceUrl(this@StatsConnectJetpackActivity)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        mDispatcher!!.register(this)
    }

    override fun onStop() {
        mDispatcher!!.unregister(this)
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
                .startJetpackConnectionFlow(this, STATS, siteModel, mAccountStore!!.hasAccessToken())
        finish()
    }

    @Subscribe(threadMode = MAIN) fun onAccountChanged(event: OnAccountChanged) {
        if (!isFinishing) {
            if (event.isError) {
                AppLog.e(
                        API, "StatsConnectJetpackActivity.onAccountChanged error: "
                        + event.error.type + " - " + event.error.message
                )
            } else if (!mIsJetpackConnectStarted && event.causeOfChange == FETCH_ACCOUNT && !TextUtils.isEmpty(
                            mAccountStore!!.account.userName
                    )) {
                startJetpackConnectionFlow(intent.getSerializableExtra(WordPress.SITE) as SiteModel)
            }
        }
    }

    companion object {
        const val ARG_CONTINUE_JETPACK_CONNECT = "ARG_CONTINUE_JETPACK_CONNECT"
        const val FAQ_URL = "https://wordpress.org/plugins/jetpack/#faq"
    }
}
