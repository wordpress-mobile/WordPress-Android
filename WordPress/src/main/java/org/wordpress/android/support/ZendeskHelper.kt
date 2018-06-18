package org.wordpress.android.support

import android.content.Context
import android.net.ConnectivityManager
import android.support.v7.preference.PreferenceManager
import android.telephony.TelephonyManager
import com.zendesk.logger.Logger
import com.zendesk.service.ErrorResponse
import com.zendesk.service.ZendeskCallback
import org.wordpress.android.WordPress
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.BuildConfig
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
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

    fun setupZendesk(
        context: Context,
        zendeskUrl: String,
        applicationId: String,
        oauthClientId: String
    ) {
        require(!isZendeskEnabled) {
            "Zendesk shouldn't be initialized more than once!"
        }
        if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
            return
        }
        zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
        Logger.setLoggable(BuildConfig.DEBUG)
        Support.INSTANCE.init(zendeskInstance)
        if (!AppPrefs.getSupportEmail().isNullOrEmpty()) {
            enablePushNotifications()
        }
    }

    /**
     * We don't force a valid identity for Help Center. If the identity is already there, we use it to enable the
     * contact us button on the Help Center, if it's not, we give the option to the user to browse the FAQ without
     * setting an email.
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

    // TODO: enable push notifications after the user creates a support identity if they haven't enabled already
    fun enablePushNotifications() {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        val preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext())
        val deviceToken = preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, null)
        if (deviceToken.isNullOrEmpty()) {
            return
        }
        zendeskInstance.provider()?.pushRegistrationProvider()?.registerWithDeviceIdentifier(
                deviceToken,
                object : ZendeskCallback<String>() {
                    override fun onSuccess(result: String?) {
                        // TODO: add logs
                    }

                    override fun onError(errorResponse: ErrorResponse?) {
                        // TODO: add logs
                    }
                })
    }

    fun disablePushNotifications() {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        zendeskInstance.provider()?.pushRegistrationProvider()?.unregisterDevice(
                object : ZendeskCallback<Void>() {
                    override fun onSuccess(response: Void) {
                        // TODO: add logs
                    }

                    override fun onError(errorResponse: ErrorResponse?) {
                        // TODO: add logs
                    }
                })
    }
}

// Helpers

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

private fun createZendeskIdentity(email: String?, name: String?): Identity =
        AnonymousIdentity.Builder().withEmailIdentifier(email).withNameIdentifier(name).build()

private fun getCombinedLogInformationOfSites(allSites: List<SiteModel>?): String {
    allSites?.let {
        return it.joinToString(separator = ZendeskConstants.blogSeparator) { it.logInformation }
    }
    return ZendeskConstants.noneValue
}

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
