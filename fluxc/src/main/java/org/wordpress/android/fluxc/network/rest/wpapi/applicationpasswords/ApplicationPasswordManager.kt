package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import android.content.Context
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.module.ApplicationName
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.UrlUtils
import javax.inject.Inject

class ApplicationPasswordManager @Inject constructor(
    context: Context,
    @ApplicationName private val applicationName: String,
    private val jetpackApplicationPasswordGenerator: JetpackApplicationPasswordGenerator,
    private val wpApiApplicationPasswordGenerator: WPApiApplicationPasswordGenerator
) {
    private val applicationPasswordsStore = ApplicationPasswordsStore(context, applicationName)

    suspend fun getApplicationCredentials(
        site: SiteModel
    ): ApplicationPasswordCreationResult {
        val existingPassword = applicationPasswordsStore.getCredentials(site.domainName)
        if (existingPassword != null) {
            return ApplicationPasswordCreationResult.Success(existingPassword)
        }

        val usernamePayload = getOrFetchUsername(site)
        if (usernamePayload.isError) {
            return ApplicationPasswordCreationResult.Failure(usernamePayload.error)
        }

        return createApplicationPassword(site, usernamePayload.userName).also {
            if (it is ApplicationPasswordCreationResult.Success) {
                applicationPasswordsStore.saveCredentials(usernamePayload.userName, it.credentials)
            }
        }
    }

    private suspend fun getOrFetchUsername(site: SiteModel): UsernameFetchPayload {
        return if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordGenerator.fetchWPAdminUsername(site)
        } else {
            UsernameFetchPayload(site.username)
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel,
        username: String
    ): ApplicationPasswordCreationResult {
        val payload = if (site.origin == SiteModel.ORIGIN_WPCOM_REST) {
            jetpackApplicationPasswordGenerator.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordGenerator.createApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return when {
            !payload.isError -> ApplicationPasswordCreationResult.Success(
                ApplicationPasswordCredentials(userName = username, password = payload.password)
            )
            else -> {
                when (payload.error.volleyError?.networkResponse?.statusCode) {
                    409 -> {
                        AppLog.w(AppLog.T.MAIN, "Application Password already exists")
                        when (val deletionResult = deleteApplicationCredentials(site)) {
                            ApplicationPasswordDeletionResult.Success ->
                                createApplicationPassword(site, username)
                            is ApplicationPasswordDeletionResult.Failure ->
                                ApplicationPasswordCreationResult.Failure(deletionResult.error)
                        }
                    }
                    404 -> {
                        AppLog.w(AppLog.T.MAIN, "Application Password feature not supported")
                        ApplicationPasswordCreationResult.NotSupported
                    }
                    else -> {
                        AppLog.w(
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
            jetpackApplicationPasswordGenerator.deleteApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        } else {
            wpApiApplicationPasswordGenerator.deleteApplicationPassword(
                site = site,
                applicationName = applicationName
            )
        }

        return when {
            !payload.isError -> {
                if (payload.isDeleted) {
                    AppLog.d(AppLog.T.MAIN, "Application password deleted")
                    deleteLocalApplicationPassword(site)
                    ApplicationPasswordDeletionResult.Success
                } else {
                    AppLog.w(AppLog.T.MAIN, "Application password deletion failed")
                    ApplicationPasswordDeletionResult.Failure(
                        BaseNetworkError(
                            UNKNOWN,
                            "Deletion not confirmed by API"
                        )
                    )
                }
            }
            else -> {
                val error = payload.error
                AppLog.w(
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