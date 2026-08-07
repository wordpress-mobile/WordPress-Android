package org.wordpress.android.ui.mysite.personalization

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ShortcutsPersonalizationViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var siteItemsBuilder: SiteItemsBuilder

    @Mock
    lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModelSlice: ShortcutsPersonalizationViewModelSlice

    private val site = SiteModel().apply { siteId = SITE_ID }

    @Before
    fun setUp() {
        viewModelSlice = ShortcutsPersonalizationViewModelSlice(
            bgDispatcher = testDispatcher(),
            siteItemsBuilder = siteItemsBuilder,
            jetpackCapabilitiesUseCase = jetpackCapabilitiesUseCase,
            blazeFeatureUtils = blazeFeatureUtils,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
        viewModelSlice.initialize(testScope())
    }

    @Test
    fun `active shortcuts lead with stats`() = test {
        givenSliceStarted()

        assertThat(viewModelSlice.uiState.value.activeShortCuts.map { it.listItemAction }).containsExactly(
            ListItemAction.STATS,
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.MEDIA
        )
    }

    @Test
    fun `enabling a non default shortcut puts it in builder order, not at the end`() = test {
        whenever(appPrefsWrapper.getShouldShowSiteItemAsQuickLink(ListItemAction.SUBSCRIBERS.toString(), SITE_ID))
            .thenReturn(true)
        givenSliceStarted()
        val comments = viewModelSlice.uiState.value.inactiveShortCuts
            .first { it.listItemAction == ListItemAction.COMMENTS }

        viewModelSlice.addShortcut(comments, SITE_ID)

        // Comments precedes Subscribers in the builder order, so it must not be appended after it.
        assertThat(viewModelSlice.uiState.value.activeShortCuts.map { it.listItemAction }).containsExactly(
            ListItemAction.STATS,
            ListItemAction.POSTS,
            ListItemAction.PAGES,
            ListItemAction.MEDIA,
            ListItemAction.COMMENTS,
            ListItemAction.SUBSCRIBERS
        )
    }

    @Test
    fun `disabling a shortcut returns it to builder order in the inactive list`() = test {
        givenSliceStarted()
        val stats = viewModelSlice.uiState.value.activeShortCuts
            .first { it.listItemAction == ListItemAction.STATS }

        viewModelSlice.removeShortcut(stats, SITE_ID)

        assertThat(viewModelSlice.uiState.value.inactiveShortCuts.map { it.listItemAction }).containsExactly(
            ListItemAction.COMMENTS,
            ListItemAction.STATS,
            ListItemAction.SUBSCRIBERS
        )
    }

    // Mirrors the Content-then-Traffic order SiteItemsBuilder produces.
    private suspend fun givenSliceStarted() {
        whenever(siteItemsBuilder.build(any())).thenReturn(
            listOf(
                listItem(ListItemAction.POSTS, R.string.my_site_btn_blog_posts),
                listItem(ListItemAction.PAGES, R.string.my_site_btn_site_pages),
                listItem(ListItemAction.MEDIA, R.string.media),
                listItem(ListItemAction.COMMENTS, R.string.my_site_btn_comments),
                listItem(ListItemAction.STATS, R.string.stats),
                listItem(ListItemAction.SUBSCRIBERS, R.string.subscribers)
            )
        )
        whenever(jetpackCapabilitiesUseCase.getJetpackPurchasedProducts(SITE_ID)).thenReturn(
            flowOf(JetpackCapabilitiesUseCase.JetpackPurchasedProducts(scan = false, backup = false))
        )
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)

        viewModelSlice.start(site)
    }

    private fun listItem(action: ListItemAction, labelRes: Int) = MySiteCardAndItem.Item.ListItem(
        primaryIcon = 0,
        primaryText = UiString.UiStringRes(labelRes),
        onClick = ListItemInteraction.create { },
        listItemAction = action
    )

    companion object {
        private const val SITE_ID = 123L
    }
}
