package org.wordpress.android.support

import android.content.Context
import android.net.ConnectivityManager
import android.telephony.TelephonyManager
import androidx.preference.PreferenceManager
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
import org.wordpress.android.util.currentLocale
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.LanguageUtils
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
import java.util.Timer
import kotlin.concurrent.schedule

private const val zendeskNeedsToBeEnabledError = "Zendesk needs to be setup before this method can be called"
private const val enablePushNotificationsDelayAfterIdentityChange: Long = 2500

class ZendeskHelper(
    private val accountStore: AccountStore,
    private val siteStore: SiteStore,
    private val supportHelper: SupportHelper,
    private val zendeskPlanFieldHelper: ZendeskPlanFieldHelper
) {
    private val zendeskInstance: Zendesk
        get() = Zendesk.INSTANCE

    private val isZendeskEnabled: Boolean
        get() = zendeskInstance.isInitialized

    private val zendeskPushRegistrationProvider: PushRegistrationProvider?
        get() = zendeskInstance.provider()?.pushRegistrationProvider()

    private val timer: Timer by lazy {
        Timer()
    }

    /**
     * These two properties are used to keep track of the Zendesk identity set. Since we allow users' to change their
     * supportEmail and reset their identity on logout, we need to ensure that the correct identity is set all times.
     * Check [requireIdentity], [refreshIdentity] & [clearIdentity] for more details about how Zendesk identity works.
     */
    private var supportEmail: String? = null
    private var supportName: String? = null

    /**
     * Although rare, Zendesk SDK might reset the identity due to a 401 error. This seems to happen if the identity
     * is changed and another Zendesk action happens before the identity change could be completed. In order to avoid
     * such issues, we check both Zendesk identity and the [supportEmail] to decide whether identity is set.
     */
    private val isIdentitySet: Boolean
        get() = !supportEmail.isNullOrEmpty() && zendeskInstance.identity != null

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
        origin: Origin?,
        selectedSite: SiteModel?,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        val builder = HelpCenterActivity.builder()
                .withArticlesForCategoryIds(ZendeskConstants.mobileCategoryId)
                .withContactUsButtonVisible(isIdentitySet)
                .withLabelNames(ZendeskConstants.articleLabel)
                .withShowConversationsMenuButton(isIdentitySet)
        AnalyticsTracker.track(Stat.SUPPORT_HELP_CENTER_VIEWED)
        if (isIdentitySet) {
            builder.show(
                context,
                buildZendeskConfig(
                    context,
                    siteStore.sites,
                    origin,
                    selectedSite,
                    extraTags,
                    zendeskPlanFieldHelper
                )
            )
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
        origin: Origin?,
        selectedSite: SiteModel?,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        requireIdentity(context, selectedSite) {
            AnalyticsTracker.track(Stat.SUPPORT_NEW_REQUEST_VIEWED)
            RequestActivity.builder()
                .show(
                    context,
                    buildZendeskConfig(
                        context,
                        siteStore.sites,
                        origin,
                        selectedSite,
                        extraTags,
                        zendeskPlanFieldHelper
                    )
                )
        }
    }

    /**
     * This function shows the user's ticket list. It'll force a valid identity, so if the user doesn't have one set,
     * a dialog will be shown where the user will need to enter an email and a name. If they cancel the dialog,
     * ticket list will not be shown. A Zendesk configuration is passed in as it's required for ticket creation.
     */
    fun showAllTickets(
        context: Context,
        origin: Origin?,
        selectedSite: SiteModel? = null,
        extraTags: List<String>? = null
    ) {
        require(isZendeskEnabled) {
            zendeskNeedsToBeEnabledError
        }
        requireIdentity(context, selectedSite) {
            AnalyticsTracker.track(Stat.SUPPORT_TICKET_LIST_VIEWED)
            RequestListActivity.builder()
                .show(
                    context,
                    buildZendeskConfig(
                        context,
                        siteStore.sites,
                        origin,
                        selectedSite,
                        extraTags,
                        zendeskPlanFieldHelper
                    )
                )
        }
    }

    /**
     * This function refreshes the Zendesk's request activity if it's currently being displayed. It'll return true if
     * it's successful. We'll use the return value to decide whether to show a push notification or not.
     */
    fun refreshRequest(context: Context, requestId: String?): Boolean =
            Support.INSTANCE.refreshRequest(requestId, context)

    /**
     * This function should be called when the user logs out of WordPress.com. Push notifications are only available
     * for WordPress.com users, so they'll be disabled. We'll also clear the Zendesk identity of the user on logout
     * and it will need to be set again when the user wants to create a new ticket.
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
        if (!isIdentitySet) {
            // identity should be set before registering the device token
            return
        }
        // The device token will not be available if the user is not logged in, so this check serves two purposes
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
        if (!isIdentitySet) {
            // identity should be set before removing the device token
            return
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
     * anonymous identity works, this will reset the users' tickets.
     */
    fun setSupportEmail(email: String?) {
        AppPrefs.setSupportEmail(email)
        refreshIdentity()
    }

    /**
     * This is a helper function which provides an easy way to make sure a Zendesk identity is set before running a
     * piece of code. It'll check the existence of the identity and call the callback if it's already available.
     * Otherwise, it'll show a dialog for the user to enter an email and name through a helper function which then
     * will be used to set the identity and call the callback. It'll also try to enable the push notifications.
     */
    private fun requireIdentity(
        context: Context,
        selectedSite: SiteModel?,
        onIdentitySet: () -> Unit
    ) {
        if (isIdentitySet) {
            // identity already available
            onIdentitySet()
            return
        }
        if (!AppPrefs.getSupportEmail().isNullOrEmpty()) {
            /**
             * Zendesk SDK reset the identity, but we already know the email of the user, we can simply refresh
             * the identity. Check out the documentation for [isIdentitySet] for more details.
             */
            refreshIdentity()
            onIdentitySet()
            return
        }
        val (emailSuggestion, nameSuggestion) = supportHelper
                .getSupportEmailAndNameSuggestion(accountStore.account, selectedSite)
        supportHelper.showSupportIdentityInputDialog(context, emailSuggestion, nameSuggestion) { email, name ->
            AppPrefs.setSupportEmail(email)
            AppPrefs.setSupportName(name)
            refreshIdentity()
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
        /**
         * We refresh the Zendesk identity if the email or the name has been updated. We also check whether
         * Zendesk SDK has cleared the identity. Check out the documentation for [isIdentitySet] for more details.
         */
        if (supportEmail != email || supportName != name || zendeskInstance.identity == null) {
            supportEmail = email
            supportName = name
            zendeskInstance.setIdentity(createZendeskIdentity(email, name))

            /**
             * When we change the identity in Zendesk, it seems to be making an asynchronous call to a server to
             * receive a different access token. During this time, if there is a call to Zendesk with the previous
             * access token, it could fail with a 401 error which seems to be clearing the identity. In order to avoid
             * such cases, we put a delay on enabling push notifications for the new identity.
             *
             * [enablePushNotifications] will check if the identity is set, before making the actual call, so if the
             * identity is cleared through [clearIdentity], this call will simply be ignored.
             */
            timer.schedule(enablePushNotificationsDelayAfterIdentityChange) {
                enablePushNotifications()
            }
        }
    }

    /**
     * This is a helper function to clear the Zendesk identity. It'll remove the credentials from AppPrefs and update
     * the Zendesk identity with a new anonymous one without an email or name. Due to the way Zendesk anonymous identity
     * works, this will clear all the users' tickets.
     */
    private fun clearIdentity() {
        AppPrefs.removeSupportEmail()
        AppPrefs.removeSupportName()
        refreshIdentity()
    }
}

// Helpers

/**
 * This is a helper function which builds a `UiConfig` through helpers to be used during ticket creation.
 */
private fun buildZendeskConfig(
    context: Context,
    allSites: List<SiteModel>?,
    origin: Origin?,
    selectedSite: SiteModel? = null,
    extraTags: List<String>? = null,
    zendeskPlanFieldHelper: ZendeskPlanFieldHelper
): UiConfig {
    return RequestActivity.builder()
        .withTicketForm(
            TicketFieldIds.form,
            buildZendeskCustomFields(context, allSites, selectedSite, zendeskPlanFieldHelper)
        )
        .withRequestSubject(ZendeskConstants.ticketSubject)
        .withTags(buildZendeskTags(allSites, origin ?: Origin.UNKNOWN, extraTags))
        .config()
}

/**
 * This is a helper function which builds a list of `CustomField`s which will be used during ticket creation. They
 * will be used to fill the custom fields we have setup in Zendesk UI for Happiness Engineers.
 */
private fun buildZendeskCustomFields(
    context: Context,
    allSites: List<SiteModel>?,
    selectedSite: SiteModel?,
    zendeskPlanFieldHelper: ZendeskPlanFieldHelper
): List<CustomField> {
    val currentSiteInformation = if (selectedSite != null) {
        "${SiteUtils.getHomeURLOrHostName(selectedSite)} (${selectedSite.stateLogInformation})"
    } else {
        "not_selected"
    }

    val customFields = arrayListOf(
        CustomField(TicketFieldIds.appVersion, PackageUtils.getVersionName(context)),
        CustomField(TicketFieldIds.blogList, getCombinedLogInformationOfSites(allSites)),
        CustomField(TicketFieldIds.currentSite, currentSiteInformation),
        CustomField(TicketFieldIds.deviceFreeSpace, DeviceUtils.getTotalAvailableMemorySize()),
        CustomField(TicketFieldIds.logs, AppLog.toPlainText(context)),
        CustomField(TicketFieldIds.networkInformation, getNetworkInformation(context)),
        CustomField(TicketFieldIds.appLanguage, LanguageUtils.getPatchedCurrentDeviceLanguage(context)),
        CustomField(TicketFieldIds.sourcePlatform, ZendeskConstants.sourcePlatform)
    )
    allSites?.let {
        val planIds = it.map { site -> site.planId }.distinct()
        val highestPlan = zendeskPlanFieldHelper.getHighestPlan(planIds)
        if (highestPlan != UNKNOWN_PLAN) {
            customFields.add(CustomField(TicketFieldIds.highestPlan, highestPlan))
        }
    }

    return customFields
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
private fun createZendeskIdentity(email: String?, name: String?): Identity {
    val identity = AnonymousIdentity.Builder()
    if (!email.isNullOrEmpty()) {
        identity.withEmailIdentifier(email)
    }
    if (!name.isNullOrEmpty()) {
        identity.withNameIdentifier(name)
    }
    return identity.build()
}

/**
 * This is a small helper function which just joins the `logInformation` of all the sites passed in with a separator.
 */
private fun getCombinedLogInformationOfSites(allSites: List<SiteModel>?): String {
    allSites?.let {
        return it.joinToString(separator = ZendeskConstants.blogSeparator) { site -> site.logInformation }
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
        if (it.any { site -> site.isWPCom }) {
            tags.add(ZendeskConstants.wpComTag)
        }

        // Add Jetpack tag if at least one site is Jetpack connected. Even if a site is Jetpack connected,
        // it does not necessarily mean that user is connected with the REST API, but we don't care about that here
        if (it.any { site -> site.isJetpackConnected }) {
            tags.add(ZendeskConstants.jetpackTag)
        }
    }
    tags.add(origin.toString())
    // Add Android tag to make it easier to filter tickets by platform
    tags.add(ZendeskConstants.platformTag)
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
            "${ZendeskConstants.networkCountryCodeLabel} ${countryCodeLabel.toUpperCase(context.currentLocale)}"
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
    // We rely on this platform tag to filter tickets in Zendesk, so should be kept separate from the `articleLabel`
    const val platformTag = "Android"
    const val sourcePlatform = "mobile_-_android"
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
    const val appLanguage = 360008583691L
    const val sourcePlatform = 360009311651L
    const val highestPlan = 25175963L
}

object ZendeskExtraTags {
    const val connectingJetpack = "connecting_jetpack"
    const val gutenbergIsDefault = "mobile_gutenberg_is_default"
}
