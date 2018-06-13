package org.wordpress.android.ui.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.MenuItem
import kotlinx.android.synthetic.main.help_activity.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.support.showSupportEmailInputDialog
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.AppLogViewerActivity
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.LocaleManager
import java.util.ArrayList
import javax.inject.Inject

class HelpActivity : AppCompatActivity() {
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var zendeskHelper: ZendeskHelper

    private val originFromExtras by lazy {
        (intent.extras?.get(HelpActivity.ORIGIN_KEY) as Origin?) ?: Origin.UNKNOWN
    }
    private val extraTagsFromExtras by lazy {
        intent.extras?.getStringArrayList(HelpActivity.EXTRA_TAGS_KEY)
    }
    private val selectedSiteFromExtras by lazy {
        intent.extras?.get(WordPress.SITE) as SiteModel?
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.help_activity)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f // remove shadow
        }

        contactUsButton.setOnClickListener { createNewZendeskTicket() }
        faqButton.setOnClickListener { showZendeskFaq() }
        myTicketsButton.setOnClickListener { showZendeskTickets() }
        applicationVersion.text = getString(R.string.version_with_name_param, WordPress.versionName)
        applicationLogButton.setOnClickListener { v ->
            startActivity(Intent(v.context, AppLogViewerActivity::class.java))
        }

        contactEmailContainer.setOnClickListener {
            showSupportEmailInputDialog(this, accountStore.account, selectedSiteFromExtras) { _ ->
                refreshContactEmailText()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ActivityId.trackLastActivity(ActivityId.HELP_SCREEN)
        refreshContactEmailText()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun createNewZendeskTicket() {
        zendeskHelper.createNewTicket(this, accountStore, siteStore, originFromExtras,
                selectedSiteFromExtras, extraTagsFromExtras)
    }

    private fun showZendeskTickets() {
        zendeskHelper.showAllTickets(this, accountStore, siteStore, originFromExtras,
                selectedSiteFromExtras, extraTagsFromExtras)
    }

    private fun showZendeskFaq() {
        zendeskHelper.showZendeskHelpCenter(this, siteStore, originFromExtras, selectedSiteFromExtras, extraTagsFromExtras)
    }

    private fun refreshContactEmailText() {
        val supportEmail = AppPrefs.getSupportEmail()
        if (!supportEmail.isNullOrEmpty()) {
            contactEmailAddress.text = supportEmail
        }
    }

    enum class Origin(private val stringValue: String) {
        UNKNOWN("origin:unknown"),
        LOGIN_SCREEN_WPCOM("origin:wpcom-login-screen"),
        LOGIN_SCREEN_SELF_HOSTED("origin:wporg-login-screen"),
        LOGIN_SCREEN_JETPACK("origin:jetpack-login-screen"),
        SIGNUP_SCREEN("origin:signup-screen"),
        ME_SCREEN_HELP("origin:me-screen-help"),
        DELETE_SITE("origin:delete-site"),
        FEEDBACK_AZTEC("origin:aztec-feedback"),
        LOGIN_EMAIL("origin:login-email"),
        LOGIN_MAGIC_LINK("origin:login-magic-link"),
        LOGIN_EMAIL_PASSWORD("origin:login-wpcom-password"),
        LOGIN_2FA("origin:login-2fa"),
        LOGIN_SITE_ADDRESS("origin:login-site-address"),
        LOGIN_SOCIAL("origin:login-social"),
        LOGIN_USERNAME_PASSWORD("origin:login-username-password"),
        RELEASE_NOTES("origin:release-notes"),
        SIGNUP_EMAIL("origin:signup-email"),
        SIGNUP_MAGIC_LINK("origin:signup-magic-link"),
        SITE_CREATION_CATEGORY("origin:site-create-site-category"),
        SITE_CREATION_THEME("origin:site-create-site-theme"),
        SITE_CREATION_DETAILS("origin:site-create-site-details"),
        SITE_CREATION_DOMAIN("origin:site-create-site-domain"),
        SITE_CREATION_CREATING("origin:site-create-creating");

        override fun toString(): String {
            return stringValue
        }
    }

    companion object {
        private const val ORIGIN_KEY = "ORIGIN_KEY"
        private const val EXTRA_TAGS_KEY = "EXTRA_TAGS_KEY"

        @JvmStatic
        fun createIntent(
            context: Context,
            origin: Origin,
            selectedSite: SiteModel?,
            extraSupportTags: List<String>?
        ): Intent {
            val intent = Intent(context, HelpActivity::class.java)
            intent.putExtra(HelpActivity.ORIGIN_KEY, origin)
            if (selectedSite != null) {
                intent.putExtra(WordPress.SITE, selectedSite)
            }
            if (extraSupportTags != null && !extraSupportTags.isEmpty()) {
                intent.putStringArrayListExtra(HelpActivity.EXTRA_TAGS_KEY, extraSupportTags as ArrayList<String>?)
            }
            return intent
        }
    }
}
