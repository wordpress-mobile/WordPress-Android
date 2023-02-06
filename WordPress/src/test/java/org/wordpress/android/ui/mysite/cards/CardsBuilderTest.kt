package org.wordpress.android.ui.mysite.cards

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickLinkRibbon
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickLinkRibbonBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quicklinksribbon.QuickLinkRibbonBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder as DashboardCardsBuilder

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest {
    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig

    @Mock
    lateinit var quickActionsCardBuilder: QuickActionsCardBuilder

    @Mock
    lateinit var quickStartCardBuilder: QuickStartCardBuilder

    @Mock
    lateinit var dashboardCardsBuilder: DashboardCardsBuilder

    @Mock
    lateinit var quickLinkRibbonBuilder: QuickLinkRibbonBuilder

    @Mock
    lateinit var site: SiteModel

    private lateinit var cardsBuilder: CardsBuilder
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
            taskType = QuickStartTaskType.CUSTOMIZE,
            uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
            completedTasks = emptyList()
        )

    @Before
    fun setUp() {
        setUpCardsBuilder()
        setUpQuickActionsBuilder()
        setUpQuickStartCardBuilder()
        setUpDashboardCardsBuilder()
        setUpQuickLinkRibbonBuilder()
    }

    /* DOMAIN REGISTRATION CARD */
    @Test
    fun `when domain credit is available, then domain card is built`() {
        val cards = buildCards(isDomainCreditAvailable = true)

        assertThat(cards.findDomainRegistrationCard()).isNotNull
    }

    @Test
    fun `when domain credit is not available, then domain card is not built`() {
        val cards = buildCards(isDomainCreditAvailable = false)

        assertThat(cards.findDomainRegistrationCard()).isNull()
    }

    /* QUICK ACTIONS CARD */

    @Test
    fun `given quick action enabled + tabs disabled, when cards built, then quick actions card is built`() {
        val cards = buildCards(isQuickActionEnabled = true, isMySiteTabsEnabled = false)

        assertThat(cards.findQuickActionsCard()).isNotNull
    }

    @Test
    fun `given quick action disabled, when cards built, then quick actions card is not built`() {
        val cards = buildCards(isQuickActionEnabled = false)

        assertThat(cards.findQuickActionsCard()).isNull()
    }

    /* QUICK START CARD */

    @Test
    fun `given quick start is not in progress, then quick start card not built`() {
        val cards = buildCards(isQuickStartInProgress = false)

        assertThat(cards.findQuickStartCard()).isNull()
    }

    @Test
    fun `given dynamic card disabled + quick start in progress, when site is selected, then QS card built`() {
        val cards = buildCards(isQuickStartDynamicCardEnabled = false, isQuickStartInProgress = true)

        assertThat(cards.findQuickStartCard()).isNotNull
    }

    @Test
    fun `given dynamic card enabled + quick start in progress, when site is selected, then QS card not built`() {
        val cards = buildCards(isQuickStartDynamicCardEnabled = true, isQuickStartInProgress = true)

        assertThat(cards.findQuickStartCard()).isNull()
    }

    /* DASHBOARD CARDS */

    @Test
    fun `when cards are built, then dashboard cards built`() {
        val cards = buildCards()

        assertThat(cards.findDashboardCards()).isNotNull
    }

    /*  QUICK LINK RIBBON */
    @Test
    fun `given tabs disabled, when cards are built, then quick link ribbon not built`() {
        val cards = buildCards(isMySiteTabsEnabled = false)

        assertThat(cards.findQuickLinkRibbon()).isNull()
    }

    @Test
    fun `given tabs enabled, when cards are built, then quick link ribbon built`() {
        val cards = buildCards(isMySiteTabsEnabled = true)

        assertThat(cards.findQuickLinkRibbon()).isNotNull
    }

    private fun List<MySiteCardAndItem>.findQuickActionsCard() =
        this.find { it is QuickActionsCard } as QuickActionsCard?

    private fun List<MySiteCardAndItem>.findQuickStartCard() = this.find { it is QuickStartCard } as QuickStartCard?

    private fun List<MySiteCardAndItem>.findDashboardCards() = this.find { it is DashboardCards }

    private fun List<MySiteCardAndItem>.findDomainRegistrationCard() =
        this.find { it is DomainRegistrationCard } as DomainRegistrationCard?

    private fun List<MySiteCardAndItem>.findQuickLinkRibbon() =
        this.find { it is QuickLinkRibbon } as QuickLinkRibbon?

    private fun buildCards(
        isQuickActionEnabled: Boolean = true,
        activeTask: QuickStartTask? = null,
        isDomainCreditAvailable: Boolean = false,
        isQuickStartInProgress: Boolean = false,
        isQuickStartDynamicCardEnabled: Boolean = false,
        isMySiteTabsEnabled: Boolean = false
    ): List<MySiteCardAndItem> {
        whenever(buildConfigWrapper.isQuickActionEnabled).thenReturn(isQuickActionEnabled)
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(isQuickStartDynamicCardEnabled)
        return cardsBuilder.build(
            quickActionsCardBuilderParams = QuickActionsCardBuilderParams(
                siteModel = site,
                onQuickActionMediaClick = mock(),
                onQuickActionPagesClick = mock(),
                onQuickActionPostsClick = mock(),
                onQuickActionStatsClick = mock()
            ),
            domainRegistrationCardBuilderParams = DomainRegistrationCardBuilderParams(
                isDomainCreditAvailable = isDomainCreditAvailable,
                domainRegistrationClick = mock()
            ),
            quickStartCardBuilderParams = QuickStartCardBuilderParams(
                if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList(),
                mock(),
                mock()
            ),
            dashboardCardsBuilderParams = DashboardCardsBuilderParams(
                onErrorRetryClick = mock(),
                todaysStatsCardBuilderParams = TodaysStatsCardBuilderParams(mock(), mock(), mock(), mock()),
                postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock()),
                bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(
                    mock(),
                    false,
                    false,
                    false,
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                )
            ),
            quickLinkRibbonBuilderParams = QuickLinkRibbonBuilderParams(
                siteModel = mock(),
                onPagesClick = mock(),
                onPostsClick = mock(),
                onMediaClick = mock(),
                onStatsClick = mock(),
                activeTask = activeTask
            ),
            isMySiteTabsEnabled
        )
    }

    private fun setUpQuickActionsBuilder() {
        doAnswer {
            initQuickActionsCard()
        }.whenever(quickActionsCardBuilder).build(any())
    }

    private fun setUpQuickStartCardBuilder() {
        doAnswer {
            initQuickStartCard()
        }.whenever(quickStartCardBuilder).build(any())
    }

    private fun setUpDashboardCardsBuilder() {
        doAnswer {
            initDashboardCards()
        }.whenever(dashboardCardsBuilder).build(any())
    }

    private fun setUpQuickLinkRibbonBuilder() {
        doAnswer {
            initQuickLinkRibbon()
        }.whenever(quickLinkRibbonBuilder).build(any())
    }

    private fun setUpCardsBuilder() {
        cardsBuilder = CardsBuilder(
            buildConfigWrapper,
            quickStartDynamicCardsFeatureConfig,
            quickActionsCardBuilder,
            quickStartCardBuilder,
            quickLinkRibbonBuilder,
            dashboardCardsBuilder
        )
    }

    private fun initQuickActionsCard() = QuickActionsCard(
        title = UiStringText(""),
        onStatsClick = mock(),
        onPagesClick = mock(),
        onPostsClick = mock(),
        onMediaClick = mock(),
        showPages = false
    )

    private fun initQuickStartCard() = QuickStartCard(
        title = UiStringText(""),
        onRemoveMenuItemClick = mock(),
        taskTypeItems = listOf(
            QuickStartTaskTypeItem(
                quickStartTaskType = mock(),
                title = UiStringText(""),
                titleEnabled = true,
                subtitle = UiStringText(""),
                strikeThroughTitle = false,
                progressColor = 0,
                progress = 0,
                onClick = mock()
            )
        )
    )

    private fun initDashboardCards() = DashboardCards(cards = mock())

    private fun initQuickLinkRibbon(): QuickLinkRibbon {
        return QuickLinkRibbon(
            quickLinkRibbonItems = mock()
        )
    }
}
