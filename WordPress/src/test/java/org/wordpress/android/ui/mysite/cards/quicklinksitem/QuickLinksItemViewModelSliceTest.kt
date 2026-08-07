package org.wordpress.android.ui.mysite.cards.quicklinksitem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.jetpack.JetpackCapabilitiesUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.cards.ListItemActionHandler
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class QuickLinksItemViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var siteItemsBuilder: SiteItemsBuilder

    @Mock
    lateinit var jetpackCapabilitiesUseCase: JetpackCapabilitiesUseCase

    @Mock
    lateinit var listItemActionHandler: ListItemActionHandler

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var viewModelSlice: QuickLinksItemViewModelSlice

    private val site = SiteModel().apply { siteId = SITE_ID }

    private var uiState: MySiteCardAndItem.Card.QuickLinksItem? = null

    @Before
    fun setUp() {
        viewModelSlice = QuickLinksItemViewModelSlice(
            selectedSiteRepository = selectedSiteRepository,
            bgDispatcher = testDispatcher(),
            siteItemsBuilder = siteItemsBuilder,
            jetpackCapabilitiesUseCase = jetpackCapabilitiesUseCase,
            listItemActionHandler = listItemActionHandler,
            blazeFeatureUtils = blazeFeatureUtils,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
        viewModelSlice.uiState.observeForever { uiState = it }
        viewModelSlice.initialization(testScope())
    }

    @Test
    fun `media is read from the default quick link pref`() = test {
        givenBuilderReturnsMenuItems()
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)

        viewModelSlice.buildCard(site)

        verify(appPrefsWrapper).getShouldShowDefaultQuickLink(ListItemAction.MEDIA.toString(), SITE_ID)
        verify(appPrefsWrapper, never()).getShouldShowSiteItemAsQuickLink(ListItemAction.MEDIA.toString(), SITE_ID)
    }

    @Test
    fun `a non default action is read from the site item quick link pref`() = test {
        givenBuilderReturnsMenuItems()
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)

        viewModelSlice.buildCard(site)

        verify(appPrefsWrapper).getShouldShowSiteItemAsQuickLink(ListItemAction.COMMENTS.toString(), SITE_ID)
        verify(appPrefsWrapper, never()).getShouldShowDefaultQuickLink(ListItemAction.COMMENTS.toString(), SITE_ID)
    }

    @Test
    fun `card leads with stats and keeps more last`() = test {
        givenBuilderReturnsMenuItems()
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)

        viewModelSlice.buildCard(site)

        assertThat(uiState!!.quickLinkItems.map { it.label }).containsExactly(
            UiString.UiStringRes(R.string.stats),
            UiString.UiStringRes(R.string.my_site_btn_blog_posts),
            UiString.UiStringRes(R.string.my_site_btn_site_pages),
            UiString.UiStringRes(R.string.media),
            UiString.UiStringRes(R.string.more)
        )
    }

    @Test
    fun `an enabled non default action keeps its builder position behind the defaults`() = test {
        givenBuilderReturnsMenuItems()
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)
        whenever(appPrefsWrapper.getShouldShowSiteItemAsQuickLink(ListItemAction.COMMENTS.toString(), SITE_ID))
            .thenReturn(true)

        viewModelSlice.buildCard(site)

        assertThat(uiState!!.quickLinkItems.map { it.label }).containsExactly(
            UiString.UiStringRes(R.string.stats),
            UiString.UiStringRes(R.string.my_site_btn_blog_posts),
            UiString.UiStringRes(R.string.my_site_btn_site_pages),
            UiString.UiStringRes(R.string.media),
            UiString.UiStringRes(R.string.my_site_btn_comments),
            UiString.UiStringRes(R.string.more)
        )
    }

    // Mirrors the Content-then-Traffic order SiteItemsBuilder produces, with Stats after Comments.
    private suspend fun givenBuilderReturnsMenuItems() {
        whenever(siteItemsBuilder.build(any())).thenReturn(
            listOf(
                listItem(ListItemAction.POSTS, R.string.my_site_btn_blog_posts),
                listItem(ListItemAction.PAGES, R.string.my_site_btn_site_pages),
                listItem(ListItemAction.MEDIA, R.string.media),
                listItem(ListItemAction.COMMENTS, R.string.my_site_btn_comments),
                listItem(ListItemAction.STATS, R.string.stats)
            )
        )
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
