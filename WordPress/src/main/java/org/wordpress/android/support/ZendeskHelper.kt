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
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.BuildConfig
import org.wordpress.android.ui.accounts.HelpActivity.Origin
import org.wordpress.android.ui.notifications.utils.NotificationsUtils
import org.wordpress.android.ui.prefs.AppPrefs
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.util.PackageUtils
import org.wordpress.android.util.SiteUtils
import org.wordpress.android.util.logInformation
import org.wordpress.android.util.stateLogInformation
import zendesk.core.AnonymousIdentity
import zendesk.core.Identity
import zendesk.core.PushRegistrationProvider
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

    private val zendeskPushRegistrationProvider: PushRegistrationProvider?
        get() = zendeskInstance.provider()?.pushRegistrationProvider()

    /**
     * These two properties are used to keep track of the Zendesk identity set. Since we allow users' to change their
     * supportEmail and reset their identity on logout, we need to ensure that the correct identity is set all times.
     * Check [requireIdentity], [refreshIdentity] & [clearIdentity] for more details about how Zendesk identity works.
     */
    private var supportEmail: String? = null
    private var supportName: String? = null
    private val isIdentityAvailable: Boolean
        get() = !supportEmail.isNullOrEmpty()

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
        refreshIdentity()
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
        requireIdentity(context, accountStore?.account, selectedSite) {
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
        requireIdentity(context, accountStore.account, selectedSite) {
            RequestListActivity.builder()
                    .show(context, buildZendeskConfig(context, siteStore, origin, selectedSite, extraTags))
        }
    }

    /**
     * This function should be called when the user logs out of WordPress.com. Push notifications are only available
     * for WordPress.com users, so they'll be disabled. We'll also clear the Zendesk identity of the user on logout.
     * The Zendesk identity will need to be set again when the user wants to create a new ticket.
     */
    fun reset() {
        disablePushNotifications()
        clearIdentity()
    }

    /**
     * This function will enable push notifications for Zendesk. Both a Zendesk identity and a valid push
     * notification device token is required. If either doesn't exist, the request will simply be ignored.
     */
    fun enablePushNotifications() {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        if (!isIdentityAvailable) {
            // identity should be set before registering the device token
            return
        }
        wpcomPushNotificationDeviceToken?.let { deviceToken ->
            zendeskPushRegistrationProvider?.registerWithDeviceIdentifier(
                    deviceToken,
                    object : ZendeskCallback<String>() {
                        override fun onSuccess(result: String?) {
                            AppLog.v(T.SUPPORT, "Zendesk push notifications successfully enabled!")
                        }

                        override fun onError(errorResponse: ErrorResponse?) {
                            AppLog.v(T.SUPPORT, "Enabling Zendesk push notifications failed with" +
                                    " error: ${errorResponse?.reason}")
                        }
                    })
        }
    }

    /**
     * This function will disable push notifications for Zendesk.
     */
    private fun disablePushNotifications() {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        zendeskPushRegistrationProvider?.unregisterDevice(
                object : ZendeskCallback<Void>() {
                    override fun onSuccess(response: Void?) {
                        AppLog.v(T.SUPPORT, "Zendesk push notifications successfully disabled!")
                    }

                    override fun onError(errorResponse: ErrorResponse?) {
                        AppLog.v(T.SUPPORT, "Disabling Zendesk push notifications failed with" +
                                " error: ${errorResponse?.reason}")
                    }
                })
    }

    /**
     * This function provides a way to change the support email for the Zendesk identity. Due to the way Zendesk
     * anonymous identity works, this will reset the users' tickets. If the user hasn't used Zendesk yet, their identity
     * might not be created. It'll attempt to enable push notifications for Zendesk for such a case.
     */
    fun setSupportEmail(email: String?) {
        AppPrefs.setSupportEmail(email)
        refreshIdentity()

        // The identity might not be available previously, this will ensure that push notifications is enabled
        enablePushNotifications()
    }

    /**
     * This is a helper function which provides an easy way to make sure a Zendesk identity is set before running a
     * piece of code. It'll check the existence of the identity and call the callback if it's already available.
     * Otherwise, it'll show a dialog for the user to enter an email and name through a helper function which then
     * will be used to set the identity and call the callback. It'll also try to enable the push notifications.
     */
    private fun requireIdentity(
        context: Context,
        account: AccountModel?,
        selectedSite: SiteModel?,
        onIdentitySet: () -> Unit
    ) {
        if (isIdentityAvailable) {
            // identity already available
            onIdentitySet()
            return
        }
        val (emailSuggestion, nameSuggestion) = supportHelper.getSupportEmailAndNameSuggestion(account, selectedSite)
        supportHelper.showSupportIdentityInputDialog(context, emailSuggestion, nameSuggestion) { email, name ->
            AppPrefs.setSupportEmail(email)
            AppPrefs.setSupportName(name)
            refreshIdentity()
            enablePushNotifications()
            onIdentitySet()
        }
    }

    /**
     * This is a helper function that'll ensure the Zendesk identity is set with the credentials from AppPrefs.
     */
    private fun refreshIdentity() {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        val email = AppPrefs.getSupportEmail()
        val name = AppPrefs.getSupportName()
        if (supportEmail != email || supportName != name) {
            supportEmail = email
            supportName = name
            zendeskInstance.setIdentity(createZendeskIdentity(email, name))
        }
    }

    /**
     * This is a helper function to clear the Zendesk identity. It'll remove the credentials from AppPrefs and update
     * the Zendesk identity with a new anonymous one without an email or name. Due to the way Zendesk anonymous identity
     * works, this will clear all the users' tickets.
     */
    private fun clearIdentity() {
        supportEmail = null
        supportName = null
        AppPrefs.removeSupportEmail()
        AppPrefs.removeSupportName()
        zendeskInstance.setIdentity(createZendeskIdentity(null, null))
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

private val wpcomPushNotificationDeviceToken: String?
    get() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(WordPress.getContext())
        return preferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_TOKEN, null)
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
