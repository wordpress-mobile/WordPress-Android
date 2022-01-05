package org.wordpress.android.ui.mysite.cards

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickActionsCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoCard.IconState
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickActionsCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.SiteInfoCardBuilderParams
import org.wordpress.android.ui.mysite.cards.quickactions.QuickActionsCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoCardBuilder
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.config.MySiteDashboardPhase2FeatureConfig
import org.wordpress.android.util.config.QuickStartDynamicCardsFeatureConfig
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder as DashboardCardsBuilder

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest {
    @Mock lateinit var buildConfigWrapper: BuildConfigWrapper
    @Mock lateinit var quickStartDynamicCardsFeatureConfig: QuickStartDynamicCardsFeatureConfig
    @Mock lateinit var siteInfoCardBuilder: SiteInfoCardBuilder
    @Mock lateinit var quickActionsCardBuilder: QuickActionsCardBuilder
    @Mock lateinit var quickStartCardBuilder: QuickStartCardBuilder
    @Mock lateinit var dashboardCardsBuilder: DashboardCardsBuilder
    @Mock lateinit var site: SiteModel
    @Mock lateinit var mySiteDashboardPhase2FeatureConfig: MySiteDashboardPhase2FeatureConfig

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
        setUpSiteInfoCardBuilder()
        setUpQuickActionsBuilder()
        setUpQuickStartCardBuilder()
        setUpDashboardCardsBuilder()
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
    fun `given mySiteDashboardPhase2 disabled, when cards are built, then dashboard cards not built`() {
        val cards = buildCards(isMySiteDashboardPhase2FeatureConfigEnabled = false)

        assertThat(cards.findDashboardCards()).isNull()
    }

    @Test
    fun `given mySiteDashboardPhase2 enabled, when cards are built, then dashboard cards built`() {
        val cards = buildCards(isMySiteDashboardPhase2FeatureConfigEnabled = true)

        assertThat(cards.findDashboardCards()).isNotNull
    }

    private fun List<MySiteCardAndItem>.findQuickActionsCard() =
            this.find { it is QuickActionsCard } as QuickActionsCard?

    private fun List<MySiteCardAndItem>.findSiteInfoCard() = this.find { it is SiteInfoCard } as SiteInfoCard?

    private fun List<MySiteCardAndItem>.findQuickStartCard() = this.find { it is QuickStartCard } as QuickStartCard?

    private fun List<MySiteCardAndItem>.findDashboardCards() = this.find { it is DashboardCards }

    private fun List<MySiteCardAndItem>.findDomainRegistrationCard() =
            this.find { it is DomainRegistrationCard } as DomainRegistrationCard?

    private fun buildCards(
        activeTask: QuickStartTask? = null,
        isDomainCreditAvailable: Boolean = false,
        isQuickStartInProgress: Boolean = false,
        isQuickStartDynamicCardEnabled: Boolean = false,
        isMySiteDashboardPhase2FeatureConfigEnabled: Boolean = false
    ): List<MySiteCardAndItem> {
        whenever(quickStartDynamicCardsFeatureConfig.isEnabled()).thenReturn(isQuickStartDynamicCardEnabled)
        whenever(mySiteDashboardPhase2FeatureConfig.isEnabled()).thenReturn(isMySiteDashboardPhase2FeatureConfigEnabled)
        return cardsBuilder.build(
                siteInfoCardBuilderParams = SiteInfoCardBuilderParams(
                        site,
                        showSiteIconProgressBar = false,
                        mock(),
                        mock(),
                        mock(),
                        mock(),
                        activeTask
                ),
                quickActionsCardBuilderParams = QuickActionsCardBuilderParams(
                        siteModel = site,
                        activeTask = activeTask,
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
                    postCardBuilderParams = PostCardBuilderParams(mock(), mock(), mock())
                )
        )
    }

    private fun setUpSiteInfoCardBuilder() {
        doAnswer {
            initSiteInfoCard(it)
        }.whenever(siteInfoCardBuilder).buildSiteInfoCard(any())
    }

    private fun setUpQuickActionsBuilder() {
        doAnswer {
            initQuickActionsCard(it)
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

    private fun setUpCardsBuilder() {
        cardsBuilder = CardsBuilder(
                buildConfigWrapper,
                quickStartDynamicCardsFeatureConfig,
                siteInfoCardBuilder,
                quickActionsCardBuilder,
                quickStartCardBuilder,
                dashboardCardsBuilder,
                mySiteDashboardPhase2FeatureConfig
        )
    }

    private fun initSiteInfoCard(mockInvocation: InvocationOnMock): SiteInfoCard {
        val params = (mockInvocation.arguments.filterIsInstance<SiteInfoCardBuilderParams>()).first()
        return SiteInfoCard(
                title = "",
                url = "",
                iconState = IconState.Visible(""),
                showTitleFocusPoint = params.activeTask == QuickStartTask.UPDATE_SITE_TITLE,
                showIconFocusPoint = params.activeTask == QuickStartTask.UPLOAD_SITE_ICON,
                onTitleClick = mock(),
                onIconClick = mock(),
                onUrlClick = mock(),
                onSwitchSiteClick = mock()
        )
    }

    private fun initQuickActionsCard(mockInvocation: InvocationOnMock): QuickActionsCard {
        val params = (mockInvocation.arguments.filterIsInstance<QuickActionsCardBuilderParams>()).first()
        return QuickActionsCard(
                title = UiStringText(""),
                onStatsClick = mock(),
                onPagesClick = mock(),
                onPostsClick = mock(),
                onMediaClick = mock(),
                showPages = false,
                showStatsFocusPoint = params.activeTask == QuickStartTask.CHECK_STATS,
                showPagesFocusPoint = params.activeTask == QuickStartTask.EDIT_HOMEPAGE ||
                        params.activeTask == QuickStartTask.REVIEW_PAGES
        )
    }

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
}
