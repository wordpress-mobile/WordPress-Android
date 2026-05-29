package org.wordpress.android.repositories

import android.util.Base64
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds the `Authorization` header that the editor — and editor
 * capability probes — use when talking to a site. Centralised so the
 * probe in [EditorSettingsRepository] cannot drift from what
 * [org.wordpress.android.ui.posts.GutenbergKitSettingsBuilder] hands
 * to GutenbergKit at launch.
 *
 * - WP.com-routed sites (JPC, Atomic, WPCom) → `Bearer <token>`
 * - Self-hosted sites with an application password → `Basic <base64>`
 *
 * Returns `null` when the required credentials are missing.
 */
@Singleton
class EditorAuthHeaderBuilder @Inject constructor() {
    /**
     * Convenience overload that derives [shouldUseWPComRestApi] and
     * credentials from [site].
     */
    fun build(site: SiteModel, accessToken: String?): String? {
        val applicationPassword = site.apiRestPasswordPlain
        val shouldUseWPComRestApi =
            applicationPassword.isNullOrEmpty() && site.isUsingWpComRestApi
        return build(
            shouldUseWPComRestApi = shouldUseWPComRestApi,
            accessToken = accessToken,
            username = site.apiRestUsernamePlain,
            password = applicationPassword,
        )
    }

    fun build(
        shouldUseWPComRestApi: Boolean,
        accessToken: String?,
        username: String?,
        password: String?,
    ): String? = if (shouldUseWPComRestApi) {
        buildBearer(accessToken)
    } else {
        buildBasic(username, password)
    }

    private fun buildBearer(accessToken: String?): String? =
        if (!accessToken.isNullOrEmpty()) {
            "$AUTH_BEARER_PREFIX$accessToken"
        } else {
            AppLog.w(
                AppLog.T.EDITOR,
                "Missing access token for WP.com REST API authentication"
            )
            null
        }

    private fun buildBasic(username: String?, password: String?): String? {
        if (username.isNullOrEmpty() || password.isNullOrEmpty()) {
            AppLog.w(
                AppLog.T.EDITOR,
                "Incomplete credentials for Basic authentication"
            )
            return null
        }
        return try {
            val credentials = "$username:$password"
            val encoded = Base64.encodeToString(
                credentials.toByteArray(Charsets.UTF_8),
                Base64.NO_WRAP
            )
            "$AUTH_BASIC_PREFIX$encoded"
        } catch (e: IllegalArgumentException) {
            AppLog.e(
                AppLog.T.EDITOR,
                "Failed to encode Basic auth credentials",
                e
            )
            null
        }
    }

    companion object {
        private const val AUTH_BEARER_PREFIX = "Bearer "
        private const val AUTH_BASIC_PREFIX = "Basic "
    }
}
