package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeCoroutineRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeCoroutineStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val themeRestClient: ThemeCoroutineRestClient,
) {
    suspend fun fetchDemoThemePages(demoThemeUrl: String): List<DemoPage> =
        coroutineEngine.withDefaultContext(
            T.API,
            this,
            "Fetching demo pages for theme $demoThemeUrl"
        ) {
            val response = themeRestClient.fetchThemeDemoPages(demoThemeUrl)
            when {
                response.isError || response.result == null -> emptyList()
                else -> response.result
                    .toList()
                    .map {
                        DemoPage(
                            link = it.link,
                            title = it.title.rendered,
                            slug = it.slug
                        )
                    }
            }
        }

    data class DemoPage(
        val link: String,
        val title: String,
        val slug: String
    )
}
