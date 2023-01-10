@file:Suppress("DEPRECATION")

package org.wordpress.android.ui.accounts

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.databinding.HelpActivityBinding
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction.FETCH_ACCOUNT
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.support.SupportHelper
import org.wordpress.android.support.ZendeskExtraTags
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.ActivityId
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.AppLogViewerActivity
import org.wordpress.android.ui.LocaleAwareActivity
import org.wordpress.android.ui.main.utils.MeGravatarLoader
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.API
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.image.ImageType.AVATAR_WITHOUT_BACKGROUND
import org.wordpress.android.viewmodel.observeEvent
import javax.inject.Inject

@AndroidEntryPoint
class HelpActivity : LocaleAwareActivity() {
    @Inject
    lateinit var accountStore: AccountStore

    @Inject
    lateinit var siteStore: SiteStore

    @Inject
    lateinit var supportHelper: SupportHelper

    @Inject
    lateinit var zendeskHelper: ZendeskHelper

    @Inject
    lateinit var meGravatarLoader: MeGravatarLoader

    @Inject
    lateinit var mDispatcher: Dispatcher

    private lateinit var binding: HelpActivityBinding

    @Suppress("DEPRECATION")
    private var signingOutProgressDialog: ProgressDialog? = null

    private val viewModel: HelpViewModel by viewModels()

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
        with(HelpActivityBinding.inflate(layoutInflater)) {
            binding = this
            setContentView(root)
            setSupportActionBar(toolbarMain)

            val actionBar = supportActionBar
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true)
                actionBar.setDisplayHomeAsUpEnabled(true)
                actionBar.elevation = 0f // remove shadow
            }

            contactUsButton.setOnClickListener { createNewZendeskTicket() }
            faqButton.setOnClickListener { showFaq() }
            myTicketsButton.setOnClickListener { showZendeskTickets() }
            applicationVersion.text = getString(R.string.version_with_name_param, WordPress.versionName)
            applicationLogButton.setOnClickListener { v ->
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
                    this@HelpActivity,
                    emailSuggestion,
                    isNameInputHidden = true
                ) { email, _ ->
                    zendeskHelper.setSupportEmail(email)
                    refreshContactEmailText()
                    AnalyticsTracker.track(Stat.SUPPORT_IDENTITY_SET)
                }
                AnalyticsTracker.track(Stat.SUPPORT_IDENTITY_FORM_VIEWED)
            }
            if (originFromExtras == Origin.JETPACK_MIGRATION_HELP) {
                configureForJetpackMigrationHelp()
            }
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
        binding.refreshContactEmailText()
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

    private fun showFaq() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://apps.wordpress.com/mobile-app-support/"))
        startActivity(intent)
        AnalyticsTracker.track(Stat.SUPPORT_HELP_CENTER_VIEWED)
    }

    private fun HelpActivityBinding.refreshContactEmailText() {
        val supportEmail = AppPrefs.getSupportEmail()
        contactEmailAddress.text = if (!supportEmail.isNullOrEmpty()) {
            supportEmail
        } else {
            getString(R.string.support_contact_email_not_set)
        }
    }

    private fun HelpActivityBinding.configureForJetpackMigrationHelp() {
        supportActionBar?.title = getString(R.string.support_title)
        faqButton.isVisible = false
        helpCenterButton.run {
            isVisible = true
            setOnClickListener { showFaq() }
        }
        applicationVersion.isVisible = false
        applicationLogButton.isVisible = false

        if (accountStore.hasAccessToken()) {
            val defaultAccount = accountStore.account
            if (TextUtils.isEmpty(defaultAccount.userName)) {
                mDispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
            } else {
                loadAccountDataForJetpackMigrationHelp(defaultAccount)
            }
        }
    }

    private fun HelpActivityBinding.loadAccountDataForJetpackMigrationHelp(account: AccountModel) {
        logOutButtonContainer.isVisible = true
        userDetailsContainer.isVisible = true
        loadAvatar(account.avatarUrl.orEmpty())
        userDisplayName.text = account.displayName.ifEmpty { account.userName }
        userName.text = getString(R.string.at_username, account.userName)
        logOutButton.setOnClickListener { logOut() }
        observeViewModelEvents()
    }

    @Subscribe(threadMode = MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (event.isError) {
            val error = "${event.error.type} - ${event.error.message}"
            AppLog.e(API, "HelpActivity.onAccountChanged error: $error")
            return
        }
        if (originFromExtras == Origin.JETPACK_MIGRATION_HELP &&
            event.causeOfChange == FETCH_ACCOUNT &&
            !TextUtils.isEmpty(accountStore.account.userName)
        ) {
            binding.loadAccountDataForJetpackMigrationHelp(accountStore.account)
        }
    }

    private fun observeViewModelEvents() {
        viewModel.showSigningOutDialog.observeEvent(this@HelpActivity) {
            when (it) {
                true -> showSigningOutDialog()
                false -> hideSigningOutDialog()
            }
        }
        viewModel.onSignOutCompleted.observe(this@HelpActivity) {
            // Load Main Activity once signed out, which launches the login flow
            ActivityLauncher.showMainActivity(this@HelpActivity)
        }
    }

    private fun HelpActivityBinding.loadAvatar(avatarUrl: String) {
        meGravatarLoader.load(
            false,
            meGravatarLoader.constructGravatarUrl(avatarUrl),
            null,
            userAvatar,
            AVATAR_WITHOUT_BACKGROUND,
            null
        )
    }

    private fun logOut() {
        viewModel.signOutWordPress(application as WordPress)
    }

    @Suppress("DEPRECATION")
    private fun showSigningOutDialog() {
        signingOutProgressDialog = ProgressDialog.show(
            this,
            null,
            getText(R.string.signing_out),
            false
        )
    }

    private fun hideSigningOutDialog() {
        if (signingOutProgressDialog?.isShowing == true) {
            signingOutProgressDialog?.dismiss()
        }
        signingOutProgressDialog = null
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
        SIGNUP_CONFIRMATION("origin:signup-confirmation"),
        SITE_CREATION_CREATING("origin:site-create-creating"),
        SITE_CREATION_SEGMENTS("origin:site-create-site-segments"),
        SITE_CREATION_VERTICALS("origin:site-create-site-verticals"),
        SITE_CREATION_DOMAINS("origin:site-create-domains"),
        SITE_CREATION_SITE_INFO("origin:site-create-site-info"),
        EDITOR_HELP("origin:editor-help"),
        SCAN_SCREEN_HELP("origin:scan-screen-help"),
        JETPACK_MIGRATION_HELP("origin:jetpack-migration-help");

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
