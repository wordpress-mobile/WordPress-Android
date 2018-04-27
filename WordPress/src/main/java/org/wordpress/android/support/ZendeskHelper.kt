package org.wordpress.android.support

import android.content.Context
import com.zendesk.sdk.feedback.BaseZendeskFeedbackConfiguration
import com.zendesk.sdk.feedback.ui.ContactZendeskActivity
import com.zendesk.sdk.model.access.AnonymousIdentity
import com.zendesk.sdk.model.access.Identity
import com.zendesk.sdk.model.request.CustomField
import com.zendesk.sdk.network.impl.ZendeskConfig
import com.zendesk.sdk.requests.RequestActivity
import com.zendesk.sdk.support.SupportActivity
import java.util.Locale

private val zendeskInstance: ZendeskConfig
    get() = ZendeskConfig.INSTANCE

val isZendeskEnabled: Boolean
    get() = zendeskInstance.isInitialized

private const val zendeskNeedsToBeEnabledError = "Zendesk needs to be setup before this method can be called"

fun setupZendesk(
    context: Context,
    zendeskUrl: String,
    applicationId: String,
    oauthClientId: String,
    email: String,
    name: String,
    deviceLocale: Locale
) {
    require(!isZendeskEnabled) {
        "Zendesk shouldn't be initialized more than once!"
    }
    if (zendeskUrl.isEmpty() || applicationId.isEmpty() || oauthClientId.isEmpty()) {
        return
    }
    zendeskInstance.init(context, zendeskUrl, applicationId, oauthClientId)
    zendeskInstance.setIdentity(zendeskIdentity(email, name))
    zendeskInstance.setDeviceLocale(deviceLocale)
}

// TODO("Make sure changing the language of the app updates the locale for Zendesk")
fun updateZendeskDeviceLocale(deviceLocale: Locale) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    zendeskInstance.setDeviceLocale(deviceLocale)
}

fun showZendeskHelpCenter(context: Context, categoryId: Long, articleLabelName: String) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    SupportActivity.Builder()
            .withArticlesForCategoryIds(categoryId)
            .withLabelNames(articleLabelName)
            .show(context)
}

fun createAndShowRequest(
    context: Context,
    zendeskFeedbackConfiguration: BaseZendeskFeedbackConfiguration,
    appVersion: String,
    blogInformation: String,
    deviceFreeSpace: String,
    logFile: String,
    networkInformation: String
) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    zendeskInstance.ticketFormId = 0
    // TODO("Use correct custom field values")
    zendeskInstance.customFields = listOf(
            CustomField(0L, appVersion),
            CustomField(0L, blogInformation),
            CustomField(0L, deviceFreeSpace),
            CustomField(0L, logFile),
            CustomField(0L, networkInformation)
    )
    ContactZendeskActivity.startActivity(context, zendeskFeedbackConfiguration)
}

fun showTicketList(context: Context, zendeskFeedbackConfiguration: BaseZendeskFeedbackConfiguration) {
    require(isZendeskEnabled) {
        zendeskNeedsToBeEnabledError
    }
    RequestActivity.startActivity(context, zendeskFeedbackConfiguration)
}

// Helpers

private fun zendeskIdentity(email: String, name: String): Identity =
        AnonymousIdentity.Builder().withEmailIdentifier(email).withNameIdentifier(name).build()
