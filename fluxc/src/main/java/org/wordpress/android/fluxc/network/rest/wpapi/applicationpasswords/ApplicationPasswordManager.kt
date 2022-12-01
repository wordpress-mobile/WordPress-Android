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

    suspend fun getApplicationPassword(
        site: SiteModel
    ): ApplicationPasswordCreationResult {
        val existingPassword = applicationPasswordsStore.getApplicationPassword(site.domainName)
        if (existingPassword != null) {
            return ApplicationPasswordCreationResult.Success(existingPassword)
        }

        return createApplicationPassword(site).also {
            if (it is ApplicationPasswordCreationResult.Success) {
                applicationPasswordsStore.saveApplicationPassword(site.domainName, it.password)
            }
        }
    }

    private suspend fun createApplicationPassword(
        site: SiteModel
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
            !payload.isError -> ApplicationPasswordCreationResult.Success(payload.password)
            else -> {
                when (payload.error.volleyError?.networkResponse?.statusCode) {
                    409 -> {
                        AppLog.w(AppLog.T.MAIN, "Application Password already exists")
                        when (val deletionResult = deleteApplicationPassword(site)) {
                            ApplicationPasswordDeletionResult.Success ->
                                createApplicationPassword(site)
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

    suspend fun deleteApplicationPassword(
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
        applicationPasswordsStore.deleteApplicationPassword(site.domainName)
    }

    private val SiteModel.domainName
        get() = UrlUtils.removeScheme(url)
}