package org.wordpress.android.repositories

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.persistence.EditorSettingsSqlUtils
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import rs.wordpress.api.kotlin.WpRequestResult
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class EditorSettingsRepository @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appPrefsWrapper: AppPrefsWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher
) {
    private val editorSettingsSqlUtils = EditorSettingsSqlUtils()

    /**
     * Returns whether the site is known to support the
     * `wp-block-editor/v1/settings` endpoint, based on
     * cached editor settings or a previously persisted
     * result from [fetchSupportsEditorSettingsForSite].
     */
    fun getSupportsEditorSettingsForSite(site: SiteModel): Boolean {
        val hasExistingEditorSettings = editorSettingsSqlUtils.getEditorSettingsForSite(site) != null
        return hasExistingEditorSettings || appPrefsWrapper.getSiteSupportsEditorSettings(site.siteId)
    }

    /**
     * Queries the site's REST API root index to check
     * whether the `wp-block-editor/v1/settings` route
     * is available. The result is persisted so that
     * [getSupportsEditorSettingsForSite] can return it
     * synchronously on future calls.
     *
     * Returns `false` if the API root request fails
     * (e.g. network error, missing application password).
     */
    suspend fun fetchSupportsEditorSettingsForSite(site: SiteModel): Boolean =
        withContext(ioDispatcher) {
            val client = wpApiClientProvider.getWpApiClient(site)
            val response = client.request { it.apiRoot().get() }

            val supports = when (response) {
                is WpRequestResult.Success ->
                    response.response.data
                        .hasRoute("wp-block-editor/v1/settings")
                else -> false
            }

            appPrefsWrapper.setSiteSupportsEditorSettings(
                site.siteId, supports
            )

            supports
        }
}
