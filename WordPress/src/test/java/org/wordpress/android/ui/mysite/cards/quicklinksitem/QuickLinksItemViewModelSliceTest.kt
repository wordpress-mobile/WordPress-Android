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
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.COMMENTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.MEDIA
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.PAGES
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.POSTS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.STATS
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.SUBSCRIBERS
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
    fun `defaults are read from the default pref and everything else from the site item pref`() = test {
        givenMenuWithDefaultsOn()

        viewModelSlice.buildCard(site)

        verify(appPrefsWrapper).getShouldShowDefaultQuickLink(MEDIA.toString(), SITE_ID)
        verify(appPrefsWrapper, never()).getShouldShowSiteItemAsQuickLink(MEDIA.toString(), SITE_ID)
        verify(appPrefsWrapper).getShouldShowSiteItemAsQuickLink(COMMENTS.toString(), SITE_ID)
        verify(appPrefsWrapper, never()).getShouldShowDefaultQuickLink(COMMENTS.toString(), SITE_ID)
    }

    @Test
    fun `card leads with stats and keeps more last`() = test {
        givenMenuWithDefaultsOn()

        viewModelSlice.buildCard(site)

        assertThat(cardLabels())
            .containsExactly(R.string.stats, R.string.my_site_btn_blog_posts, R.string.my_site_btn_site_pages,
                R.string.media, R.string.more)
    }

    @Test
    fun `enabled non defaults follow the defaults, keeping their order relative to each other`() = test {
        givenMenuWithDefaultsOn()
        whenever(appPrefsWrapper.getShouldShowSiteItemAsQuickLink(COMMENTS.toString(), SITE_ID)).thenReturn(true)
        whenever(appPrefsWrapper.getShouldShowSiteItemAsQuickLink(SUBSCRIBERS.toString(), SITE_ID)).thenReturn(true)

        viewModelSlice.buildCard(site)

        // Comments precedes Subscribers in the builder order and must stay that way behind the defaults.
        assertThat(cardLabels())
            .containsExactly(R.string.stats, R.string.my_site_btn_blog_posts, R.string.my_site_btn_site_pages,
                R.string.media, R.string.my_site_btn_comments, R.string.subscribers, R.string.more)
    }

    @Test
    fun `a default the user turned off is left out of the card`() = test {
        givenMenuWithDefaultsOn()
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(MEDIA.toString(), SITE_ID)).thenReturn(false)

        viewModelSlice.buildCard(site)

        assertThat(cardLabels())
            .containsExactly(R.string.stats, R.string.my_site_btn_blog_posts, R.string.my_site_btn_site_pages,
                R.string.more)
    }

    private fun cardLabels() = uiState!!.quickLinkItems.map { it.label.stringRes }

    // Content then Traffic, the order SiteItemsBuilder produces, with every default turned on.
    private suspend fun givenMenuWithDefaultsOn() {
        val menu = listOf(
            POSTS to R.string.my_site_btn_blog_posts,
            PAGES to R.string.my_site_btn_site_pages,
            MEDIA to R.string.media,
            COMMENTS to R.string.my_site_btn_comments,
            STATS to R.string.stats,
            SUBSCRIBERS to R.string.subscribers
        )
        whenever(siteItemsBuilder.build(any())).thenReturn(
            menu.map { (action, label) -> listItem(action, label) }
        )
        whenever(appPrefsWrapper.getShouldShowDefaultQuickLink(any(), any())).thenReturn(true)
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
