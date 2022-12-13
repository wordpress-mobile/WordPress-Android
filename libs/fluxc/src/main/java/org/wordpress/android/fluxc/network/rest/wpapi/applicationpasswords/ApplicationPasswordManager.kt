package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ApplicationPasswordClientId
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

private const val CONFLICT = 409
private const val NOT_FOUND = 404
private const val APPLICATION_PASSWORDS_DISABLED_ERROR_CODE = "application_passwords_disabled"

internal class ApplicationPasswordManager @Inject constructor(
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    @ApplicationPasswordClientId private val applicationName: String,
    private val jetpackApplicationPasswordRestClient: JetpackApplicationPasswordRestClient,
    private val wpApiApplicationPasswordRestClient: WPApiApplicationPasswordRestClient,
    private val appLogWrapper: AppLogWrapper
) {
    @Suppress("ReturnCount")
    suspend fun getApplicationCredentials(
        site: SiteModel
    ): ApplicationPasswordCreationResult {
        if (site.isWPCom) return ApplicationPasswordCreationResult.NotSupported(
            WPAPINetworkError(
                BaseNetworkError(
                    GenericErrorType.UNKNOWN,
                    "Simple WPCom sites don't support application passwords"
                )
            )
        )
        val existingPassword = applicationPasswordsStore.getCredentials(site.domainName)
        if (existingPassword != null) {
            return ApplicationPasswordCreationResult.Existing(existingPassword)
        }

        val usernamePayload = getOrFetchUsername(site)
        return if (usernamePayload.isError) {
            ApplicationPasswordCreationResult.Failure(usernamePayload.error)
        } else {
            createApplicationPassword(site, usernamePayload.userName).also {
                if (it is ApplicationPasswordCreationResult.Created) {
                    applicationPasswordsStore.saveCredentials(
                        usernamePayload.userName,
                        it.credentials
                    )
                }
            }
        }
    }

    private suspend fun getOrFetchUsername(site: SiteModel): UsernameFetchPayload {
        return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordRestClient.fetchWPAdminUsername(site)
        } else {
            UsernameFetchPayload(site.username)
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel,
        username: String
    ): ApplicationPasswordCreationResult {
        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return when {
            !payload.isError -> ApplicationPasswordCreationResult.Created(
                ApplicationPasswordCredentials(userName = username, password = payload.password)
            )
            else -> {
                val statusCode = payload.error.volleyError?.networkResponse?.statusCode
                val errorCode = payload.error.let {
                    when (it) {
                        is WPComGsonNetworkError -> it.apiError
                        is WPAPINetworkError -> it.errorCode
                        else -> null
                    }
                }
                when {
                    statusCode == CONFLICT -> {
                        appLogWrapper.w(AppLog.T.MAIN, "Application Password already exists")
                        when (val deletionResult = deleteApplicationCredentials(site)) {
                            ApplicationPasswordDeletionResult.Success ->
                                createApplicationPassword(site, username)
                            is ApplicationPasswordDeletionResult.Failure ->
                                ApplicationPasswordCreationResult.Failure(deletionResult.error)
                        }
                    }
                    statusCode == NOT_FOUND ||
                        errorCode == APPLICATION_PASSWORDS_DISABLED_ERROR_CODE -> {
                        appLogWrapper.w(
                            AppLog.T.MAIN,
                            "Application Password feature not supported, " +
                                "status code: $statusCode, errorCode: $errorCode"
                        )
                        ApplicationPasswordCreationResult.NotSupported(payload.error)
                    }
                    else -> {
                        appLogWrapper.w(
                            AppLog.T.MAIN,
                            "Application Password creation failed ${payload.error.type}"
                        )
                        ApplicationPasswordCreationResult.Failure(payload.error)
                    }
                }
            }
        }
    }

    suspend fun deleteApplicationCredentials(
        site: SiteModel
    ): ApplicationPasswordDeletionResult {
        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordRestClient.deleteApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordRestClient.deleteApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return when {
            !payload.isError -> {
                if (payload.isDeleted) {
                    appLogWrapper.d(AppLog.T.MAIN, "Application password deleted")
                    deleteLocalApplicationPassword(site)
                    ApplicationPasswordDeletionResult.Success
                } else {
                    appLogWrapper.w(AppLog.T.MAIN, "Application password deletion failed")
                    ApplicationPasswordDeletionResult.Failure(
                        BaseNetworkError(
                            GenericErrorType.UNKNOWN,
                            "Deletion not confirmed by API"
                        )
                    )
                }
            }
            else -> {
                val error = payload.error
                appLogWrapper.w(
                    AppLog.T.MAIN, "Application password deletion failed, error: " +
                    "${error.type} ${error.message}\n" +
                    "${error.volleyError?.toString()}"
                )
                ApplicationPasswordDeletionResult.Failure(error)
            }
        }
    }

    fun deleteLocalApplicationPassword(site: SiteModel) {
        applicationPasswordsStore.deleteCredentials(site.domainName)
    }

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url)
}
