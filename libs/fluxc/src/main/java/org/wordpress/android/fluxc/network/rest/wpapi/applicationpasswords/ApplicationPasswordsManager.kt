package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MAIN
import javax.inject.Inject

private const val UNAUTHORIZED = 401
private const val CONFLICT = 409
private const val NOT_FOUND = 404
private const val APPLICATION_PASSWORDS_DISABLED_ERROR_CODE = "application_passwords_disabled"

internal class ApplicationPasswordsManager @Inject constructor(
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val jetpackApplicationPasswordsRestClient: JetpackApplicationPasswordsRestClient,
    private val wpApiApplicationPasswordsRestClient: WPApiApplicationPasswordsRestClient,
    private val configuration: ApplicationPasswordsConfiguration,
    private val appLogWrapper: AppLogWrapper
) {
    private val applicationName
        get() = configuration.applicationName

    /**
     * Checks whether the site supports creating new Application Passwords using the API, and the different cases are:
     * 1. For Jetpack sites, we always can call the API using the WordPress.com token.
     * 2. For self-hosted sites, we need to check if we have persisted credentials, otherwise we can't create it. This
     *    case happens when a site's Application Password was saved directly to the [ApplicationPasswordsStore],
     *    which happens during the Web Authorization.
     */
    private val SiteModel.supportsApplicationPasswordsGeneration
        get() = origin == SiteModel.ORIGIN_WPCOM_REST ||
            (!username.isNullOrEmpty() && !password.isNullOrEmpty())

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
        if (!site.supportsApplicationPasswordsGeneration) {
            ApplicationPasswordCreationResult.Failure(
                WPAPINetworkError(
                    BaseNetworkError(
                        GenericErrorType.NOT_AUTHENTICATED,
                        "Site password is missing. " +
                            "The application password was probably authorized using the Web flow",
                        VolleyError(
                            NetworkResponse(
                                UNAUTHORIZED, null, true, System.currentTimeMillis(), emptyList()
                            )
                        )
                    )
                )
            )
        }

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

        return handleApplicationPasswordCreationResult(site, username, payload)
    }

    private suspend fun handleApplicationPasswordCreationResult(
        site: SiteModel,
        username: String,
        payload: ApplicationPasswordCreationPayload,
    ): ApplicationPasswordCreationResult {
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
                        appLogWrapper.w(MAIN, "Application Password already exists")
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
                            MAIN,
                            "Application Password feature not supported, " +
                                "status code: $statusCode, errorCode: $errorCode"
                        )
                        ApplicationPasswordCreationResult.NotSupported(payload.error)
                    }

                    else -> {
                        appLogWrapper.w(
                            MAIN,
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
        val credentials = applicationPasswordsStore.getCredentials(site)

        val payload = if (credentials == null) {
            // If we don't have any saved credentials, let's fetch the UUID then delete the password using
            // either the WP.com token or the self-hosted credentials.
            val uuid = fetchApplicationPasswordUUID(site).let {
                if (it.isError) return ApplicationPasswordDeletionResult.Failure(it.error)
                it.uuid
            }

            if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
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
        } else {
            // If we have an Application Password, we can use it itself for the delete request.
            wpApiApplicationPasswordsRestClient.deleteApplicationPassword(
                site = site,
                credentials = credentials
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
