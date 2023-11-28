package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeCoroutineRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject

class ThemeCoroutineStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val themeRestClient: ThemeCoroutineRestClient,
) {
    fun fetchDemoThemePages(demoThemeUrl: String) =
        coroutineEngine.launch(T.PLUGINS, this, "Fetching demo pages for theme $demoThemeUrl") {
           themeRestClient.fetchThemeDemoPages(demoThemeUrl)
        }
}