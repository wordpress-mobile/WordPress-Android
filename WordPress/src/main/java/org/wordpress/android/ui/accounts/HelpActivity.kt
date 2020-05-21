package org.wordpress.android.ui.accounts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import kotlinx.android.synthetic.main.help_activity.*
import kotlinx.android.synthetic.main.toolbar_main.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.support.SupportHelper
import org.wordpress.android.support.ZendeskExtraTags
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.AppLogViewerActivity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.SiteUtils
import java.util.ArrayList
import javax.inject.Inject

class HelpActivity : LocaleAwareActivity() {
    @Inject lateinit var accountStore: AccountStore
    @Inject lateinit var siteStore: SiteStore
    @Inject lateinit var supportHelper: SupportHelper
    @Inject lateinit var zendeskHelper: ZendeskHelper

    private val originFromExtras by lazy {
        (intent.extras?.get(ORIGIN_KEY) as Origin?) ?: Origin.UNKNOWN
    }
    private val extraTagsFromExtras by lazy {
        intent.extras?.getStringArrayList(EXTRA_TAGS_KEY)
    }
    private val selectedSiteFromExtras by lazy {
        intent.extras?.get(WordPress.SITE) as SiteModel?
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (application as WordPress).component().inject(this)

        setContentView(R.layout.help_activity)

        setSupportActionBar(toolbar_main)

        val actionBar = supportActionBar
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.elevation = 0f // remove shadow
        }

        contact_us_button.setOnClickListener { createNewZendeskTicket() }
        faq_button.setOnClickListener { showZendeskFaq() }
        my_tickets_button.setOnClickListener { showZendeskTickets() }
        applicationVersion.text = getString(R.string.version_with_name_param, WordPress.versionName)
        application_log_button.setOnClickListener { v ->
            startActivity(Intent(v.context, AppLogViewerActivity::class.java))
        }

        contactEmailContainer.setOnClickListener {
            var emailSuggestion = AppPrefs.getSupportEmail()
            if (emailSuggestion.isNullOrEmpty()) {
                emailSuggestion = supportHelper
                        .getSupportEmailAndNameSuggestion(
                                accountStore.account,
                                selectedSiteFromExtras
                        ).first
            }

            supportHelper.showSupportIdentityInputDialog(
                    this,
                    emailSuggestion,
                    isNameInputHidden = true
            ) { email, _ ->
                zendeskHelper.setSupportEmail(email)
                refreshContactEmailText()
                AnalyticsTracker.track(Stat.SUPPORT_IDENTITY_SET)
            }
            AnalyticsTracker.track(Stat.SUPPORT_IDENTITY_FORM_VIEWED)
        }

        /**
         * If the user taps on a Zendesk notification, we want to show them the `My Tickets` page. However, this
         * should only be triggered when the activity is first created, otherwise if the user comes back from
         * `My Tickets` and rotates the screen (or triggers the activity re-creation in any other way) it'll navigate
         * them to `My Tickets` again since the `originFromExtras` will still be [Origin.ZENDESK_NOTIFICATION].
         */
        if (savedInstanceState == null && originFromExtras == Origin.ZENDESK_NOTIFICATION) {
            showZendeskTickets()
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
        zendeskHelper.createNewTicket(
                this,
                originFromExtras,
                selectedSiteFromExtras,
                extraTagsFromExtras
        )
    }

    private fun showZendeskTickets() {
        zendeskHelper.showAllTickets(
                this,
                originFromExtras,
                selectedSiteFromExtras,
                extraTagsFromExtras
        )
    }

    private fun showZendeskFaq() {
        zendeskHelper
                .showZendeskHelpCenter(
                        this,
                        originFromExtras,
                        selectedSiteFromExtras,
                        extraTagsFromExtras
                )
    }

    private fun refreshContactEmailText() {
        val supportEmail = AppPrefs.getSupportEmail()
        contactEmailAddress.text = if (!supportEmail.isNullOrEmpty()) {
            supportEmail
        } else {
            getString(R.string.support_contact_email_not_set)
        }
    }

    enum class Origin(private val stringValue: String) {
        UNKNOWN("origin:unknown"),
        ZENDESK_NOTIFICATION("origin:zendesk-notification"),
        LOGIN_SCREEN_WPCOM("origin:wpcom-login-screen"),
        LOGIN_SCREEN_SELF_HOSTED("origin:wporg-login-screen"),
        LOGIN_SCREEN_JETPACK("origin:jetpack-login-screen"),
        SIGNUP_SCREEN("origin:signup-screen"),
        ME_SCREEN_HELP("origin:me-screen-help"),
        DELETE_SITE("origin:delete-site"),
        DISCARD_CHANGES("origin:discard-changes"),
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
        SITE_CREATION_CREATING("origin:site-create-creating"),
        SITE_CREATION_SEGMENTS("origin:site-create-site-segments"),
        SITE_CREATION_VERTICALS("origin:site-create-site-verticals"),
        SITE_CREATION_DOMAINS("origin:site-create-domains"),
        SITE_CREATION_SITE_INFO("origin:site-create-site-info");

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
            intent.putExtra(ORIGIN_KEY, origin)
            if (selectedSite != null) {
                intent.putExtra(WordPress.SITE, selectedSite)
            }

            // construct a mutable list to add the related and extra tags
            val tagsList = ArrayList<String>()

            // add the provided list of tags if any
            extraSupportTags?.let {
                tagsList.addAll(extraSupportTags)
            }

            // Append the "mobile_gutenberg_is_default" tag if gutenberg is set to default for new posts
            if (SiteUtils.isBlockEditorDefaultForNewPost(selectedSite)) {
                tagsList.add(ZendeskExtraTags.gutenbergIsDefault)
            }

            if (tagsList.isNotEmpty()) {
                intent.putStringArrayListExtra(EXTRA_TAGS_KEY, tagsList)
            }

            return intent
        }
    }
}
