package org.wordpress.android.ui.posts

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.SiteSettingsProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.repositories.EditorSettingsRepository
import org.wordpress.android.ui.accounts.login.SiteApiRestUrlRecoverer
import org.wordpress.gutenberg.model.EditorAssetBundle
import org.wordpress.gutenberg.model.EditorConfiguration
import org.wordpress.gutenberg.model.EditorDependencies
import org.wordpress.gutenberg.model.EditorSettings
import org.wordpress.gutenberg.model.PostTypeDetails

@ExperimentalCoroutinesApi
class GutenbergEditorPreloaderTest :
    BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    lateinit var appContext: Context

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var gutenbergKitFeatureChecker: GutenbergKitFeatureChecker

    @Mock
    lateinit var gutenbergKitSettingsBuilder: GutenbergKitSettingsBuilder

    @Mock
    lateinit var siteSettingsProvider: SiteSettingsProvider

    @Mock
    lateinit var editorServiceProvider: EditorServiceProvider

    @Mock
    lateinit var editorSettingsRepository: EditorSettingsRepository

    @Mock
    lateinit var siteApiRestUrlRecoverer: SiteApiRestUrlRecoverer

    private val editorDependencies = EditorDependencies.empty

    private lateinit var preloader: GutenbergEditorPreloader

    private fun createSite(id: Int = 1): SiteModel {
        val site = SiteModel()
        site.id = id
        site.name = "Site $id"
        site.url = "https://example.test"
        return site
    }

    @Before
    fun setUp() {
        preloader = GutenbergEditorPreloader(
            appContext = appContext,
            accountStore = accountStore,
            gutenbergKitFeatureChecker = gutenbergKitFeatureChecker,
            gutenbergKitSettingsBuilder = gutenbergKitSettingsBuilder,
            siteSettingsProvider = siteSettingsProvider,
            editorServiceProvider = editorServiceProvider,
            editorSettingsRepository = editorSettingsRepository,
            siteApiRestUrlRecoverer = siteApiRestUrlRecoverer,
            bgDispatcher = testDispatcher()
        )
    }

    private fun enablePreloading(site: SiteModel) {
        whenever(
            gutenbergKitFeatureChecker.isGutenbergKitEnabled()
        ).thenReturn(true)
        whenever(
            siteSettingsProvider.isBlockEditorDefault(site)
        ).thenReturn(true)
    }

    private fun stubSuccessfulPreload() {
        whenever(
            gutenbergKitSettingsBuilder.buildPostConfiguration(
                site = any(),
                accessToken = anyOrNull(),
                cookies = any(),
                isNetworkLoggingEnabled = any(),
                post = anyOrNull(),
            )
        ).thenReturn(stubEditorConfiguration())
    }

    private fun stubEditorConfiguration(): EditorConfiguration =
        EditorConfiguration.builder(
            siteURL = "https://example.test",
            siteApiRoot = "https://example.test/wp-json/",
            postType = PostTypeDetails.post,
        ).build()

    private suspend fun stubEditorService() {
        whenever(
            editorServiceProvider.prepare(
                context = any(),
                configuration = anyOrNull(),
                coroutineScope = any()
            )
        ).thenReturn(editorDependencies)
    }

    // region getDependencies

    @Test
    fun `getDependencies returns null when nothing preloaded`() {
        val site = createSite()
        assertThat(preloader.getDependencies(site)).isNull()
    }

    @Test
    fun `getDependencies by ID returns null when nothing preloaded`() {
        assertThat(preloader.getDependencies(99)).isNull()
    }

    // endregion

    // region preloadIfNeeded — gating

    @Test
    fun `skips preload when feature is disabled`() = test {
        val site = createSite()
        whenever(
            gutenbergKitFeatureChecker.isGutenbergKitEnabled()
        ).thenReturn(false)

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site)).isNull()
        verify(editorServiceProvider, never()).prepare(
            context = any(),
            configuration = any(),
            coroutineScope = any()
        )
    }

    @Test
    fun `skips preload when block editor is not default`() = test {
        val site = createSite()
        whenever(
            gutenbergKitFeatureChecker.isGutenbergKitEnabled()
        ).thenReturn(true)
        whenever(
            siteSettingsProvider.isBlockEditorDefault(site)
        ).thenReturn(false)

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site)).isNull()
        verify(editorServiceProvider, never()).prepare(
            context = any(),
            configuration = any(),
            coroutineScope = any()
        )
    }

    // endregion

    // region preloadIfNeeded — success

    @Test
    fun `successful preload caches dependencies`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site))
            .isSameAs(editorDependencies)
    }

    @Test
    fun `successful preload fetches editor capabilities`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        verify(editorSettingsRepository)
            .fetchEditorCapabilitiesForSite(site)
    }

    @Test
    fun `getDependencies by ID returns cached result`() = test {
        val site = createSite(id = 42)
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(42))
            .isSameAs(editorDependencies)
    }

    // endregion

    // region preloadIfNeeded — failure

    @Test
    fun `failed preload removes entry`() = test {
        val site = createSite()
        enablePreloading(site)
        whenever(
            gutenbergKitSettingsBuilder.buildPostConfiguration(
                site = any(),
                accessToken = anyOrNull(),
                cookies = any(),
                isNetworkLoggingEnabled = any(),
                post = anyOrNull(),
            )
        ).thenThrow(RuntimeException("network error"))

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site)).isNull()
    }

    // endregion

    // region deduplication

    @Test
    fun `second preload for same site is skipped`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()
        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        verify(editorServiceProvider).prepare(
            context = any(),
            configuration = anyOrNull(),
            coroutineScope = any()
        )
    }

    @Test
    fun `in-flight preload blocks duplicate request`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        // Job is in-flight — don't advance
        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        verify(editorServiceProvider).prepare(
            context = any(),
            configuration = anyOrNull(),
            coroutineScope = any()
        )
    }

    @Test
    fun `getDependencies returns null while preload is in-flight`() =
        test {
            val site = createSite()
            enablePreloading(site)
            stubSuccessfulPreload()
            stubEditorService()

            preloader.preloadIfNeeded(site, this)
            // Job is in-flight — don't advance

            assertThat(preloader.getDependencies(site)).isNull()
        }

    @Test
    fun `cancelled scope allows fresh preload attempt`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        // Launch in a separate scope and cancel it
        val expendableScope = TestScope(testDispatcher())
        preloader.preloadIfNeeded(site, expendableScope)
        expendableScope.cancel()

        // The dead Loading entry should not block a retry
        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site))
            .isSameAs(editorDependencies)
    }

    // endregion

    // region multi-site caching

    @Test
    fun `preloading site B does not discard site A`() = test {
        val siteA = createSite(id = 1)
        val siteB = createSite(id = 2)
        enablePreloading(siteA)
        enablePreloading(siteB)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(siteA, this)
        advanceUntilIdle()
        preloader.preloadIfNeeded(siteB, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(siteA))
            .isSameAs(editorDependencies)
        assertThat(preloader.getDependencies(siteB))
            .isSameAs(editorDependencies)
    }

    @Test
    fun `concurrent in-flight preloads for different sites`() =
        test {
            val siteA = createSite(id = 1)
            val siteB = createSite(id = 2)
            enablePreloading(siteA)
            enablePreloading(siteB)
            stubSuccessfulPreload()
            stubEditorService()

            preloader.preloadIfNeeded(siteA, this)
            preloader.preloadIfNeeded(siteB, this)
            // Both in-flight — now advance
            advanceUntilIdle()

            assertThat(preloader.getDependencies(siteA))
                .isSameAs(editorDependencies)
            assertThat(preloader.getDependencies(siteB))
                .isSameAs(editorDependencies)
        }

    // endregion

    // region refreshPreloading

    @Test
    fun `refresh discards cached result and re-preloads`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        // Now make the service return a different result
        val freshDependencies = EditorDependencies(
            editorSettings = EditorSettings.undefined,
            assetBundle = EditorAssetBundle.empty,
            preloadList = null
        )
        whenever(
            editorServiceProvider.prepare(
                context = any(),
                configuration = anyOrNull(),
                coroutineScope = any()
            )
        ).thenReturn(freshDependencies)

        preloader.refreshPreloading(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site))
            .isSameAs(freshDependencies)
    }

    @Test
    fun `failed refresh removes previously cached result`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()
        assertThat(preloader.getDependencies(site)).isNotNull

        // Make the refresh fail
        whenever(
            gutenbergKitSettingsBuilder.buildPostConfiguration(
                site = any(),
                accessToken = anyOrNull(),
                cookies = any(),
                isNetworkLoggingEnabled = any(),
                post = anyOrNull(),
            )
        ).thenThrow(RuntimeException("refresh failed"))

        preloader.refreshPreloading(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site)).isNull()
    }

    @Test
    fun `refresh on never-preloaded site works`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.refreshPreloading(site, this)
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site))
            .isSameAs(editorDependencies)
    }

    // endregion

    // region clear

    @Test
    fun `clear during in-flight preload discards result`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        // Job is in-flight — clear before it completes
        preloader.clear()
        advanceUntilIdle()

        assertThat(preloader.getDependencies(site)).isNull()
    }

    @Test
    fun `clear removes all cached dependencies`() = test {
        val siteA = createSite(id = 1)
        val siteB = createSite(id = 2)
        enablePreloading(siteA)
        enablePreloading(siteB)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(siteA, this)
        preloader.preloadIfNeeded(siteB, this)
        advanceUntilIdle()

        preloader.clear()

        assertThat(preloader.getDependencies(siteA)).isNull()
        assertThat(preloader.getDependencies(siteB)).isNull()
    }

    // endregion

    // region wpApiRestUrl recovery

    @Test
    fun `successful preload invokes discovery only — slice owns persistence`() = test {
        val site = createSite()
        enablePreloading(site)
        stubSuccessfulPreload()
        stubEditorService()

        preloader.preloadIfNeeded(site, this)
        advanceUntilIdle()

        verify(siteApiRestUrlRecoverer).discoverApiRootUrl(site.url)
        verify(siteApiRestUrlRecoverer, never()).persistApiRootUrl(any(), any())
    }

    // endregion
}
