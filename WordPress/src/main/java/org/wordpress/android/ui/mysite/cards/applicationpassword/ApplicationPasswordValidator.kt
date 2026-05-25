package org.wordpress.android.ui.mysite.cards.applicationpassword

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.RequestExecutionErrorReason
import javax.inject.Inject

/**
 * Validates that the SiteModel's application-password credentials still work against the site's
 * direct host. Uses [WpApiClientProvider.getApplicationPasswordClient] so the call exercises the
 * application password specifically — `getWpApiClient` would route WPCom-flagged sites through the
 * bearer-token path and would not catch a revoked password.
 */
class ApplicationPasswordValidator @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
) {
    suspend fun validate(site: SiteModel): Outcome {
        appLogWrapper.d(
            AppLog.T.MAIN,
            "A_P: Validating application password for ${site.url} as user='${site.apiRestUsernamePlain}'"
        )
        return try {
            val client = wpApiClientProvider.getApplicationPasswordClient(site)
            val response = client.request { it.users().retrieveMeWithViewContext() }
            appLogWrapper.d(AppLog.T.MAIN, "A_P: Validation response: ${response::class.simpleName}")
            when (response) {
                is WpRequestResult.Success -> {
                    val user = response.response.data
                    appLogWrapper.d(
                        AppLog.T.MAIN,
                        "A_P: Validation Success returned user id=${user.id}, slug='${user.slug}', name='${user.name}'"
                    )
                    Outcome.Valid
                }
                is WpRequestResult.WpError -> Outcome.Invalid
                is WpRequestResult.UnknownError -> Outcome.Invalid
                is WpRequestResult.RequestExecutionFailed ->
                    if (response.reason is RequestExecutionErrorReason.HttpTimeoutError) {
                        Outcome.NetworkUnavailable
                    } else {
                        Outcome.Invalid
                    }
                else -> Outcome.Invalid
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            appLogWrapper.e(
                AppLog.T.MAIN,
                "A_P: Validation exception for ${site.url}: ${e::class.simpleName}: ${e.message}"
            )
            Outcome.NetworkUnavailable
        }
    }

    enum class Outcome { Valid, Invalid, NetworkUnavailable }
}
