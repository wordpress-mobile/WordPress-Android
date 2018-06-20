package org.wordpress.android.support

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import com.zendesk.logger.Logger
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.BuildConfig
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.logInformation
import org.wordpress.android.util.stateLogInformation
import zendesk.core.AnonymousIdentity
import zendesk.core.Identity
import zendesk.core.Zendesk
import zendesk.support.CustomField
import zendesk.support.Support
import zendesk.support.UiConfig
import zendesk.support.guide.HelpCenterActivity
import zendesk.support.request.RequestActivity
import zendesk.support.requestlist.RequestListActivity

private const val zendeskNeedsToBeEnabledError = "Zendesk needs to be setup before this method can be called"

class ZendeskHelper(private val supportHelper: SupportHelper) {
    private val zendeskInstance: Zendesk
        get() = Zendesk.INSTANCE

    private val isZendeskEnabled: Boolean
        get() = zendeskInstance.isInitialized

    /**
     * This function sets up the Zendesk singleton instance with the passed in credentials. This step is required
     * for the rest of Zendesk functions to work and it should only be called once, probably during the Application
     * setup. It'll also enable Zendesk logs for DEBUG builds.
     */
    @JvmOverloads
    fun setupZendesk(
        context: Context,
        zendeskUrl: String,
        applicationId: String,
        oauthClientId: String,
        enableLogs: Boolean = BuildConfig.DEBUG
    ) {
        require(!isZendeskEnabled) {
            "Zendesk shouldn't be initialized more than once!"
        }
        if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
            return
        }
        zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
        Logger.setLoggable(enableLogs)
        Support.INSTANCE.init(zendeskInstance)
    }

    /**
     * This function shows the Zendesk Help Center. It doesn't require a valid identity. If the support identity is
     * available it'll be used and the "New Ticket" button will be available, if not, it'll work with an anonymous
     * identity. The configuration will only be passed in if the identity is available, as it's only required if
     * the user contacts us through it.
     */
    fun showZendeskHelpCenter(
        context: Context,
        siteStore: SiteStore,
        origin: Origin?,
        selectedSite: SiteModel?,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        val supportEmail = AppPrefs.getSupportEmail()
        val supportName = AppPrefs.getSupportName()
        val isIdentityAvailable = !supportEmail.isNullOrEmpty()
        if (isIdentityAvailable) {
            zendeskInstance.setIdentity(createZendeskIdentity(supportEmail, supportName))
        } else {
            zendeskInstance.setIdentity(createZendeskIdentity(null, null))
        }
        val builder = HelpCenterActivity.builder()
                .withArticlesForCategoryIds(ZendeskConstants.mobileCategoryId)
                .withContactUsButtonVisible(isIdentityAvailable)
                .withLabelNames(ZendeskConstants.articleLabel)
                .withShowConversationsMenuButton(isIdentityAvailable)
        AnalyticsTracker.track(Stat.SUPPORT_HELP_CENTER_VIEWED)
        if (isIdentityAvailable) {
            builder.show(context, buildZendeskConfig(context, siteStore, origin, selectedSite, extraTags))
        } else {
            builder.show(context)
        }
    }

    /**
     * This function creates a new ticket. It'll force a valid identity, so if the user doesn't have one set, a dialog
     * will be shown where the user will need to enter an email and a name. If they cancel the dialog, the ticket
     * creation will be canceled as well. A Zendesk configuration is passed in as it's required for ticket creation.
     */
    @JvmOverloads
    fun createNewTicket(
        context: Context,
        accountStore: AccountStore?,
        siteStore: SiteStore,
        origin: Origin?,
        selectedSite: SiteModel?,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        supportHelper.getSupportIdentity(context, accountStore?.account, selectedSite) { email, name ->
            zendeskInstance.setIdentity(createZendeskIdentity(email, name))
            RequestActivity.builder()
                    .show(context, buildZendeskConfig(context, siteStore, origin, selectedSite, extraTags))
        }
    }

    /**
     * This function shows the user's ticket list. It'll force a valid identity, so if the user doesn't have one set,
     * a dialog will be shown where the user will need to enter an email and a name. If they cancel the dialog,
     * ticket list will not be shown. A Zendesk configuration is passed in as it's required for ticket creation.
     */
    fun showAllTickets(
        context: Context,
        accountStore: AccountStore,
        siteStore: SiteStore,
        origin: Origin?,
        selectedSite: SiteModel? = null,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        supportHelper.getSupportIdentity(context, accountStore.account, selectedSite) { email, name ->
            zendeskInstance.setIdentity(createZendeskIdentity(email, name))
            RequestListActivity.builder()
                    .show(context, buildZendeskConfig(context, siteStore, origin, selectedSite, extraTags))
        }
    }
}

// Helpers

/**
 * This is a helper function which builds a `UiConfig` through helpers to be used during ticket creation.
 */
private fun buildZendeskConfig(
    context: Context,
    siteStore: SiteStore,
    origin: Origin?,
    selectedSite: SiteModel? = null,
    extraTags: List<String>? = null
): UiConfig {
    return RequestActivity.builder()
            .withTicketForm(TicketFieldIds.form, buildZendeskCustomFields(context, siteStore, selectedSite))
            .withRequestSubject(ZendeskConstants.ticketSubject)
            .withTags(buildZendeskTags(siteStore.sites, origin ?: Origin.UNKNOWN, extraTags))
            .config()
}

/**
 * This is a helper function which builds a list of `CustomField`s which will be used during ticket creation. They
 * will be used to fill the custom fields we have setup in Zendesk UI for Happiness Engineers.
 */
private fun buildZendeskCustomFields(
    context: Context,
    siteStore: SiteStore,
    selectedSite: SiteModel?
): List<CustomField> {
    val currentSiteInformation = if (selectedSite != null) {
        "${SiteUtils.getHomeURLOrHostName(selectedSite)} (${selectedSite.stateLogInformation})"
    } else {
        "not_selected"
    }
    return listOf(
            CustomField(TicketFieldIds.appVersion, PackageUtils.getVersionName(context)),
            CustomField(TicketFieldIds.blogList, getCombinedLogInformationOfSites(siteStore.sites)),
            CustomField(TicketFieldIds.currentSite, currentSiteInformation),
            CustomField(TicketFieldIds.deviceFreeSpace, DeviceUtils.getTotalAvailableMemorySize()),
            CustomField(TicketFieldIds.logs, AppLog.toPlainText(context)),
            CustomField(TicketFieldIds.networkInformation, getNetworkInformation(context))
    )
}

/**
 * This is a helper function which creates an anonymous Zendesk identity with the email and name passed in. They can
 * both be `null` as they are not required for a valid identity.
 *
 * An important thing to note is that whenever a different set of values are passed in, a different identity will be
 * created which will reset the ticket list for the user. So, for example, even if the passed in email is the same,
 * if the name is different, it'll reset Zendesk's local DB.
 *
 * This is currently the way we handle identity for Zendesk, but it's possible that we may switch to a JWT based
 * authentication which will avoid the resetting issue, but will mean that we'll need to involve our own servers in the
 * authentication. More information can be found in their documentation:
 * https://developer.zendesk.com/embeddables/docs/android-support-sdk/sdk_set_identity#setting-a-unique-identity
 */
private fun createZendeskIdentity(email: String?, name: String?): Identity =
        AnonymousIdentity.Builder().withEmailIdentifier(email).withNameIdentifier(name).build()

/**
 * This is a small helper function which just joins the `logInformation` of all the sites passed in with a separator.
 */
private fun getCombinedLogInformationOfSites(allSites: List<SiteModel>?): String {
    allSites?.let {
        return it.joinToString(separator = ZendeskConstants.blogSeparator) { it.logInformation }
    }
    return ZendeskConstants.noneValue
}

/**
 * This is a helper function which returns a set of pre-defined tags depending on some conditions. It accepts a list of
 * custom tags to be added for special cases.
 */
private fun buildZendeskTags(allSites: List<SiteModel>?, origin: Origin, extraTags: List<String>?): List<String> {
    val tags = ArrayList<String>()
    allSites?.let {
        // Add wpcom tag if at least one site is WordPress.com site
        if (it.any { it.isWPCom }) {
            tags.add(ZendeskConstants.wpComTag)
        }

        // Add Jetpack tag if at least one site is Jetpack connected. Even if a site is Jetpack connected,
        // it does not necessarily mean that user is connected with the REST API, but we don't care about that here
        if (it.any { it.isJetpackConnected }) {
            tags.add(ZendeskConstants.jetpackTag)
        }

        // Find distinct plans and add them
        val plans = it.mapNotNull { it.planShortName }.distinct()
        tags.addAll(plans)
    }
    tags.add(origin.toString())
    extraTags?.let {
        tags.addAll(it)
    }
    return tags
}

/**
 * This is a helper function which returns information about the network state of the app to be sent to Zendesk, which
 * could prove useful for the Happiness Engineers while debugging the users' issues.
 */
private fun getNetworkInformation(context: Context): String {
    val networkType = when (NetworkUtils.getActiveNetworkInfo(context)?.type) {
        ConnectivityManager.TYPE_WIFI -> ZendeskConstants.networkWifi
        ConnectivityManager.TYPE_MOBILE -> ZendeskConstants.networkWWAN
        else -> ZendeskConstants.unknownValue
    }
    val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
    val carrierName = telephonyManager?.networkOperatorName ?: ZendeskConstants.unknownValue
    val countryCodeLabel = telephonyManager?.networkCountryIso ?: ZendeskConstants.unknownValue
    return listOf(
            "${ZendeskConstants.networkTypeLabel} $networkType",
            "${ZendeskConstants.networkCarrierLabel} $carrierName",
            "${ZendeskConstants.networkCountryCodeLabel} ${countryCodeLabel.toUpperCase()}"
    ).joinToString(separator = "\n")
}

private object ZendeskConstants {
    const val articleLabel = "Android"
    const val blogSeparator = "\n----------\n"
    const val jetpackTag = "jetpack"
    const val mobileCategoryId = 360000041586
    const val networkWifi = "WiFi"
    const val networkWWAN = "Mobile"
    const val networkTypeLabel = "Network Type:"
    const val networkCarrierLabel = "Carrier:"
    const val networkCountryCodeLabel = "Country Code:"
    const val noneValue = "none"
    const val ticketSubject = "WordPress for Android Support"
    const val wpComTag = "wpcom"
    const val unknownValue = "unknown"
}

private object TicketFieldIds {
    const val appVersion = 360000086866L
    const val blogList = 360000087183L
    const val deviceFreeSpace = 360000089123L
    const val form = 360000010286L
    const val logs = 22871957L
    const val networkInformation = 360000086966L
    const val currentSite = 360000103103L
}

object ZendeskExtraTags {
    const val connectingJetpack = "connecting_jetpack"
}
