package org.wordpress.android.ui.mysite.cards.dashboard.activity

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.ActivityCardModel
import org.wordpress.android.fluxc.network.rest.wpcom.dashboard.CardsUtils
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardError
import org.wordpress.android.fluxc.store.dashboard.CardsStore.ActivityCardErrorType
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DashboardCards.DashboardCard.ActivityCard
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.ActivityCardBuilderParams
import org.wordpress.android.util.DateTimeUtilsWrapper

@ExperimentalCoroutinesApi
class ActivityCardBuilderTest : BaseUnitTest() {
    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

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
        builder = ActivityCardBuilder(dateTimeUtilsWrapper)
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn(displayDate)
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
    fun `given feature flag enabled, when build is called, then card is returned`() {
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(any())).thenReturn(displayDate)

        val result = buildActivityCard(activityCardModel)

        assertThat(result).isNotNull
    }

    @Test
    fun `given activities list size is greater than 3, when build is called, then only 3 activities are selected`() {
        val activityModelWithFiveItems = ActivityCardModel(activities = List(5) { activityLogModel })

        val result = buildActivityCard(activityModelWithFiveItems)

        assertThat((result as ActivityCard.ActivityCardWithItems).activityItems.size).isEqualTo(maxItemsInCard)
    }

    private fun buildActivityCard(model: ActivityCardModel) = builder.build(
        ActivityCardBuilderParams(
            activityCardModel = model,
            onFooterLinkClick = onActivityCardFooterLinkClick,
            onActivityItemClick = onActivityItemClick
        )
    )
}
