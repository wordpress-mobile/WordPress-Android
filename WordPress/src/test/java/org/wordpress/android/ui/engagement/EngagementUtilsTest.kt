package org.wordpress.android.ui.engagement

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.ui.engagement.EngageItem.Liker
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import org.wordpress.android.ui.engagement.utils.getDefaultLikers
import org.wordpress.android.ui.engagement.utils.isEqualTo

@RunWith(MockitoJUnitRunner::class)
class EngagementUtilsTest {
    private lateinit var engagementUtils: EngagementUtils

    private val numLikers = 10
    private val siteId = 100L
    private val postId = 1000L

    @Before
    fun setup() {
        engagementUtils = EngagementUtils()
    }

    @Test
    fun `like models are mapped to engage items`() {
        val likersList = getDefaultLikers(numLikers, POST_LIKE, siteId, postId)

        val engageItems = engagementUtils.likesToEngagedPeople(likersList)

        assertThat(likersList.isEqualTo(engageItems)).isTrue
    }

    @Test
    fun `like models maps onClick function`() {
        val likersList = getDefaultLikers(numLikers, POST_LIKE, siteId, postId)
        val onClickFunction = ::onClickDummy

        val engageItems = engagementUtils.likesToEngagedPeople(
                likersList,
                onClickFunction
        )

        assertThat(engageItems.all { (it as Liker).onClick == onClickFunction }).isTrue
    }

    @Test
    fun `like models maps source`() {
        val likersList = getDefaultLikers(numLikers, POST_LIKE, siteId, postId)
        val onClickFunction = ::onClickDummy
        val source = EngagementNavigationSource.LIKE_READER_LIST

        val engageItems = engagementUtils.likesToEngagedPeople(
                likersList,
                onClickFunction,
                source
        )

        assertThat(engageItems.all { (it as Liker).source == source }).isTrue
    }

    @Suppress("unused")
    private fun onClickDummy(userProfile: UserProfile, source: EngagementNavigationSource?) = Unit
}
