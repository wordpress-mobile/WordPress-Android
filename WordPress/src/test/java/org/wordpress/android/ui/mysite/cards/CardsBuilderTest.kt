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
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.ui.mysite.MySiteCardAndItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard.QuickStartTaskTypeItem
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BlazeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloganuaryNudgeCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.BloggingPromptCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardPlansBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DashboardCardsBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.DomainRegistrationCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.JetpackInstallFullPluginCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PagesCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.PostCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.QuickStartCardBuilderParams
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.TodaysStatsCardBuilderParams
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardBuilder
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartCardType
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.ui.mysite.cards.dashboard.CardsBuilder as DashboardCardsBuilder

@RunWith(MockitoJUnitRunner::class)
class CardsBuilderTest {
    @Mock
    lateinit var quickStartCardBuilder: QuickStartCardBuilder

    @Mock
    lateinit var dashboardCardsBuilder: DashboardCardsBuilder

    @Mock
    lateinit var jetpackInstallFullPluginCardBuilder: JetpackInstallFullPluginCardBuilder

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
        setUpQuickStartCardBuilder()
        setUpDashboardCardsBuilder()
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

    /* QUICK START CARD */

    @Test
    fun `given quick start is not in progress, then quick start card not built`() {
        val cards = buildCards(isQuickStartInProgress = false)

        assertThat(cards.findQuickStartCard()).isNull()
    }

    @Test
    fun `given quick start in progress, when site is selected, then QS card built`() {
        val cards = buildCards(isQuickStartInProgress = true)

        assertThat(cards.findQuickStartCard()).isNotNull
    }

    /* DASHBOARD CARDS */

    @Test
    fun `when cards are built, then dashboard cards built`() {
        val cards = buildCards()

        assertThat(cards).isNotNull
    }

    private fun List<MySiteCardAndItem>.findQuickStartCard() = this.find { it is QuickStartCard } as QuickStartCard?

    private fun List<MySiteCardAndItem>.findDomainRegistrationCard() =
        this.find { it is DomainRegistrationCard } as DomainRegistrationCard?

    @Suppress("LongMethod")
    private fun buildCards(
        isDomainCreditAvailable: Boolean = false,
        isEligibleForPlansCard: Boolean = false,
        isQuickStartInProgress: Boolean = false,
    ): List<MySiteCardAndItem> {
        return cardsBuilder.build(
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
                bloganuaryNudgeCardBuilderParams = BloganuaryNudgeCardBuilderParams(
                    false,
                    mock(),
                    mock(),
                    mock(),
                ),
                bloggingPromptCardBuilderParams = BloggingPromptCardBuilderParams(
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                    mock(),
                ),
                blazeCardBuilderParams = BlazeCardBuilderParams.PromoteWithBlazeCardBuilderParams(
                    mock(),
                    mock()
                ),
                dashboardCardPlansBuilderParams = DashboardCardPlansBuilderParams(
                    isEligible = isEligibleForPlansCard,
                    mock(),
                    mock(),
                    mock()
                ),
                pagesCardBuilderParams = PagesCardBuilderParams(mock(), mock(), mock(), mock()),
                activityCardBuilderParams = ActivityCardBuilderParams(mock(), mock(), mock(), mock(), mock())
            ),
            jetpackInstallFullPluginCardBuilderParams = JetpackInstallFullPluginCardBuilderParams(
                site = site,
                onLearnMoreClick = mock(),
                onHideMenuItemClick = mock(),
            )
        )
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
            quickStartCardBuilder,
            dashboardCardsBuilder,
            jetpackInstallFullPluginCardBuilder,
        )
    }

    private fun initQuickStartCard() = QuickStartCard(
        title = UiStringText(""),
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
        ),
        quickStartCardType = QuickStartCardType.NEXT_STEPS,
        moreMenuOptions = mock()
    )

    private fun initDashboardCards() = mutableListOf<MySiteCardAndItem.Card>()
}
