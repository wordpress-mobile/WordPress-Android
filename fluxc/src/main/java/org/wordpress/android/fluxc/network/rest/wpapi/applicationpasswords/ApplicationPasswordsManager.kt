package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ApplicationPasswordsClientId
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import java.util.Optional
import javax.inject.Inject

private const val CONFLICT = 409
private const val NOT_FOUND = 404
private const val APPLICATION_PASSWORDS_DISABLED_ERROR_CODE = "application_passwords_disabled"

/**
 * Note: the [ApplicationPasswordsClientId] is provided as [Optional] because we want to keep the feature optional and
 * to not force the client apps to provide it. With this change, we will keep Dagger happy, and we move from a compile
 * error to a runtime error if it's missing.
 */
internal class ApplicationPasswordsManager @Inject constructor(
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val jetpackApplicationPasswordsRestClient: JetpackApplicationPasswordsRestClient,
    private val wpApiApplicationPasswordsRestClient: WPApiApplicationPasswordsRestClient,
    private val configuration: ApplicationPasswordsConfiguration,
    private val appLogWrapper: AppLogWrapper
) {
    private val applicationName
        get() = configuration.applicationName

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
        val existingPassword = applicationPasswordsStore.getCredentials(site)
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
                        site,
                        it.credentials
                    )
                }
            }
        }
    }

    private suspend fun getOrFetchUsername(site: SiteModel): UsernameFetchPayload {
        return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.fetchWPAdminUsername(site)
        } else {
            UsernameFetchPayload(site.username)
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel,
        username: String
    ): ApplicationPasswordCreationResult {
        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordsRestClient.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return when {
            !payload.isError -> ApplicationPasswordCreationResult.Created(
                ApplicationPasswordCredentials(
                    userName = username,
                    password = payload.password,
                    uuid = payload.uuid
                )
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
        val uuid = applicationPasswordsStore.getCredentials(site)?.uuid
            ?: fetchApplicationPasswordUUID(site).let {
                if (it.isError) return ApplicationPasswordDeletionResult.Failure(it.error)
                it.uuid
            }

        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.deleteApplicationPassword(
                site = site,
                uuid = uuid
            )
        } else {
            wpApiApplicationPasswordsRestClient.deleteApplicationPassword(
                site = site,
                uuid = uuid
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

    private suspend fun fetchApplicationPasswordUUID(
        site: SiteModel
    ): ApplicationPasswordUUIDFetchPayload {
        return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName)
        } else {
            wpApiApplicationPasswordsRestClient.fetchApplicationPasswordUUID(site, applicationName)
        }
    }

    fun deleteLocalApplicationPassword(site: SiteModel) {
        applicationPasswordsStore.deleteCredentials(site)
    }
}
