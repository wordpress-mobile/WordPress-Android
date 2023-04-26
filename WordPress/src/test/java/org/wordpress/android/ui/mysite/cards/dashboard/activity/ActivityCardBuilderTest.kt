package org.wordpress.android.ui.mysite.cards.dashboard.activity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.ActivityCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardErrorType
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import org.wordpress.android.util.config.DashboardCardActivityLogConfig

@ExperimentalCoroutinesApi
class ActivityCardBuilderTest : BaseUnitTest() {
    @Mock
    private lateinit var dashboardCardActivityLogConfig: DashboardCardActivityLogConfig

    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    private lateinit var siteUtilsWrapper: SiteUtilsWrapper

    @Mock
    private lateinit var siteModel: SiteModel

    private lateinit var builder: ActivityCardBuilder

    private val maxItemsInCard = 3
    private val displayDate = "X days ago"
    private val activityLogModel = ActivityLogModel(
        activityID = "1",
        summary = "summary",
        content = FormattableContent(text = "text"),
        gridicon = "gridicon",
        status = "OK",
        published = CardsUtils.fromDate("2021-12-27 11:33:55"),
        actor = ActivityLogModel.ActivityActor(
            avatarURL = "avatarURL",
            displayName = "name",
            type = "type",
            role = "admin",
            wpcomUserID = 1L
        ),
        name = "name",
        rewindable = false,
        rewindID = "1234",
        type = "type",
    )

    private val activityCardModel = ActivityCardModel(activities = listOf(activityLogModel))

    private val onActivityCardFooterLinkClick: () -> Unit = {}
    private val onActivityItemClick: (ActivityCardBuilderParams.ActivityCardItemClickParams) -> Unit = {}

    @Before
    fun setUp() {
        builder = ActivityCardBuilder(dashboardCardActivityLogConfig, dateTimeUtilsWrapper, siteUtilsWrapper)
    }

    @Test
    fun `given activity error, when build is called, then null is returned`() {
        val activity = ActivityCardModel(error = ActivityCardError(ActivityCardErrorType.UNAUTHORIZED))

        val result = buildActivityCard(activity)

        assertThat(result).isNull()
    }

    @Test
    fun `given generic error, when build is called, then null is returned`() {
        val activity = ActivityCardModel(error = ActivityCardError(ActivityCardErrorType.GENERIC_ERROR))

        val result = buildActivityCard(activity)

        assertThat(result).isNull()
    }

    @Test
    fun `given feature flag is disabled, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(false)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNull()
    }

    @Test
    fun `given activities list is empty, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        val activity = ActivityCardModel(activities = emptyList())

        val result = buildActivityCard(activity)

        assertThat(result).isNull()
    }

    @Test
    fun `given site accessed is not via wpComOrJetpack, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(any())).thenReturn(false)
        whenever(siteModel.isJetpackConnected).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteModel.isWpForTeamsSite).thenReturn(true)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNull()
    }

    @Test
    fun `given site is not Jetpack connected, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteModel.isJetpackConnected).thenReturn(false)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNull()
    }

    @Test
    fun `given does not hasCapabilityManageOptions for site, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(any())).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(false)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNull()
    }

    @Test
    fun `given is wp for teams site, when build is called, then null is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(any())).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteModel.isWpForTeamsSite).thenReturn(true)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNull()
    }

    @Test
    fun `given feature flag enabled, when build is called, then card is returned`() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(any())).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteModel.isWpForTeamsSite).thenReturn(false)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn(displayDate)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNotNull
    }

    @Test
    fun `given activities list size is greater than 3, when build is called, then only 3 activities are selected`() {
        setShouldBuildActivityCard()
        val activityModelWithFiveItems = ActivityCardModel(activities = List(5) { activityLogModel })

        val result = buildActivityCard(activityModelWithFiveItems)

        assertThat((result as ActivityCard.ActivityCardWithItems).activityItems.size).isEqualTo(maxItemsInCard)
    }

    private fun setShouldBuildActivityCard() {
        whenever(dashboardCardActivityLogConfig.isEnabled()).thenReturn(true)
        whenever(siteUtilsWrapper.isAccessedViaWPComRest(any())).thenReturn(true)
        whenever(siteModel.hasCapabilityManageOptions).thenReturn(true)
        whenever(siteModel.isWpForTeamsSite).thenReturn(false)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn(displayDate)
    }

    private fun buildActivityCard(model: ActivityCardModel) = builder.build(
        ActivityCardBuilderParams(
            site = siteModel,
            activityCardModel = model,
            onFooterLinkClick = onActivityCardFooterLinkClick,
            onActivityItemClick = onActivityItemClick
        )
    )
}
