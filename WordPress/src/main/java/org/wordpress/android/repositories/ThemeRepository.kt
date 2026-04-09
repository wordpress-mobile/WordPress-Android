package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.modules.IO_THREAD
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.ThemeListParams
import uniffi.wp_api.ThemeStatus
import uniffi.wp_api.ThemeWithEditContext
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class ThemeRepository @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    /**
     * Fetches the current active theme for the given site
     * via the `wp/v2/themes?status=active` endpoint.
     */
    suspend fun fetchCurrentTheme(site: SiteModel): ThemeWithEditContext? =
        withContext(ioDispatcher) {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request {
                it.themes().listWithEditContext(ThemeListParams(
                    status = ThemeStatus.Active
                ))
            }

            when (response) {
                is WpRequestResult.Success ->
                    response.response.data.firstOrNull()
                else -> null
            }
        }
}
