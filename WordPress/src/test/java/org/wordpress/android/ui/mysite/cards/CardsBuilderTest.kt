package org.wordpress.android.ui.mysite.cards

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.analytics.AnalyticsTracker.Stat.DOMAIN_CREDIT_PROMPT_SHOWN
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard.IconState
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig

private const val SITE_INFO_SHOW_UPDATE_SITE_TITLE_FOCUS_POINT_PARAM_POSITION = 6
private const val SITE_INFO_SHOW_UPDATE_SITE_ICON_FOCUS_POINT_PARAM_POSITION = 7
private const val QUICK_ACTION_SHOW_PAGES_FOCUS_POINT_PARAM_POSITION = 5
private const val QUICK_ACTION_SHOW_STATS_FOCUS_POINT_PARAM_POSITION = 6

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest {
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var siteInfoCardBuilder: SiteInfoCardBuilder
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var quickActionsCardBuilder: QuickActionsCardBuilder
    @Mock lateinit var quickStartCardBuilder: QuickStartCardBuilder
    @Mock lateinit var site: SiteModel

    private lateinit var cardsBuilder: CardsBuilder

    @Before
    fun setUp() {
        setUpCardsBuilder()
        setUpSiteInfoCardBuilder()
        setUpQuickActionsBuilder()
    }

    /* SITE INFO CARD */
    @Test
    fun `when active task is update site title, then title focus point is shown in the site info card`() {
        val cards = buildCards(activeTask = QuickStartTask.UPDATE_SITE_TITLE)

        assertThat(cards.findSiteInfoCard()?.showTitleFocusPoint).isTrue
    }

    @Test
    fun `when active task is update site icon, then icon focus point is shown in the site info card`() {
        val cards = buildCards(activeTask = QuickStartTask.UPLOAD_SITE_ICON)

        assertThat(cards.findSiteInfoCard()?.showIconFocusPoint).isTrue
    }

    /* DOMAIN REGISTRATION CARD */
    @Test
    fun `when domain credit is available, then correct event is tracked`() {
        buildCards(isDomainCreditAvailable = true)

        verify(analyticsTrackerWrapper).track(DOMAIN_CREDIT_PROMPT_SHOWN)
    }

    /* QUICK ACTIONS CARD */
    @Test
    fun `when build is Jetpack, then quick action card is not built`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)
        val cards = buildCards()

        assertThat(cards.findQuickActionsCard()).isNull()
    }

    @Test
    fun `when build is WordPress, then quick action card is built`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)
        val cards = buildCards()

        assertThat(cards.findQuickActionsCard()).isNotNull
    }

    private fun List<MySiteCardAndItem>.findQuickActionsCard() =
            this.find { it is QuickActionsCard } as QuickActionsCard?

    private fun List<MySiteCardAndItem>.findSiteInfoCard() = this.find { it is SiteInfoCard } as SiteInfoCard?

    private fun buildCards(
        activeTask: QuickStartTask? = null,
        isDomainCreditAvailable: Boolean = false
    ) = cardsBuilder.build(
            site = site,
            showSiteIconProgressBar = false,
            activeTask = activeTask,
            isDomainCreditAvailable = isDomainCreditAvailable,
            quickStartCategories = emptyList(),
            titleClick = mock(),
            iconClick = mock(),
            urlClick = mock(),
            switchSiteClick = mock(),
            quickActionStatsClick = mock(),
            quickActionPagesClick = mock(),
            quickActionPostsClick = mock(),
            quickActionMediaClick = mock(),
            domainRegistrationClick = mock(),
            onQuickStartBlockRemoveMenuItemClick = mock(),
            onQuickStartTaskTypeItemClick = mock()
    )

    fun setUpSiteInfoCardBuilder() {
        doAnswer {
            val siteInfoCard = SiteInfoCard(
                    title = "",
                    url = "",
                    iconState = IconState.Visible(""),
                    showTitleFocusPoint = it.getArgument(SITE_INFO_SHOW_UPDATE_SITE_TITLE_FOCUS_POINT_PARAM_POSITION),
                    showIconFocusPoint = it.getArgument(SITE_INFO_SHOW_UPDATE_SITE_ICON_FOCUS_POINT_PARAM_POSITION),
                    onTitleClick = mock(),
                    onIconClick = mock(),
                    onUrlClick = mock(),
                    onSwitchSiteClick = mock()
            )
            siteInfoCard
        }.whenever(siteInfoCardBuilder).buildSiteInfoCard(
                site = eq(site),
                showSiteIconProgressBar = any(),
                titleClick = any(),
                iconClick = any(),
                urlClick = any(),
                switchSiteClick = any(),
                showUpdateSiteTitleFocusPoint = any(),
                showUploadSiteIconFocusPoint = any()
        )
    }

    fun setUpQuickActionsBuilder() {
        doAnswer {
            val siteInfoCard = QuickActionsCard(
                    title = UiStringText(""),
                    onStatsClick = mock(),
                    onPagesClick = mock(),
                    onPostsClick = mock(),
                    onMediaClick = mock(),
                    showPages = false,
                    showPagesFocusPoint = it.getArgument(QUICK_ACTION_SHOW_PAGES_FOCUS_POINT_PARAM_POSITION),
                    showStatsFocusPoint = it.getArgument(QUICK_ACTION_SHOW_STATS_FOCUS_POINT_PARAM_POSITION)
            )
            siteInfoCard
        }.whenever(quickActionsCardBuilder).build(
                onQuickActionStatsClick = any(),
                onQuickActionPagesClick = any(),
                onQuickActionPostsClick = any(),
                onQuickActionMediaClick = any(),
                showPages = any(),
                showStatsFocusPoint = any(),
                showPagesFocusPoint = any()
        )
    }

    private fun setUpCardsBuilder() {
        cardsBuilder = CardsBuilder(
                buildConfigWrapper,
                quickStartDynamicCardsFeatureConfig,
                siteInfoCardBuilder,
                analyticsTrackerWrapper,
                quickActionsCardBuilder,
                quickStartCardBuilder
        )
    }
}
