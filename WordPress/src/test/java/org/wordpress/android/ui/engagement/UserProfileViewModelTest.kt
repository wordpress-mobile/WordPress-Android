package org.wordpress.android.ui.engagement

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.engagement.BottomSheetAction.HideBottomSheet
import org.wordpress.android.ui.engagement.BottomSheetAction.ShowBottomSheet
import org.wordpress.android.ui.engagement.BottomSheetUiState.UserProfileUiState
import org.wordpress.android.ui.engagement.EngagedListNavigationEvent.OpenUserProfileBottomSheet.UserProfile
import org.wordpress.android.ui.engagement.EngagementNavigationSource.LIKE_NOTIFICATION_LIST
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

class UserProfileViewModelTest : BaseUnitTest() {
    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var viewModel: UserProfileViewModel

    private var sheetAction: BottomSheetAction? = null
    private var sheetUiState: BottomSheetUiState? = null

    @Before
    fun setup() {
        viewModel = UserProfileViewModel(
                resourceProvider,
                analyticsUtilsWrapper
        )

        setupObservers()
    }

    @Test
    fun `bottom sheet ui state is updated when user clicks on a liker`() {
        val userProfile = getDefaultUserProfile()
        viewModel.onBottomSheetOpen(userProfile, mock(), LIKE_NOTIFICATION_LIST)

        requireNotNull(sheetUiState).let {
            assertThat(it is UserProfileUiState).isTrue
            with(it as UserProfileUiState) {
                assertThat(userAvatarUrl).isEqualTo(userProfile.userAvatarUrl)
                assertThat(blavatarUrl).isEqualTo(userProfile.blavatarUrl)
                assertThat(userName).isEqualTo(userProfile.userName)
                assertThat(userLogin).isEqualTo(userProfile.userLogin)
                assertThat(userBio).isEqualTo(userProfile.userBio)
                assertThat(siteTitle).isEqualTo(userProfile.siteTitle)
                assertThat(siteUrl).isEqualTo(userProfile.siteUrl)
                assertThat(siteId).isEqualTo(userProfile.siteId)
                assertThat(blogPreviewSource).isEqualTo(ReaderTracker.SOURCE_NOTIF_LIKE_LIST_USER_PROFILE)
            }
        }
    }

    @Test
    fun `bottom sheet is opened when user clicks on a liker`() {
        val userProfile = getDefaultUserProfile()
        viewModel.onBottomSheetOpen(userProfile, mock(), LIKE_NOTIFICATION_LIST)

        requireNotNull(sheetAction).let {
            assertThat(it is ShowBottomSheet).isTrue
        }
    }

    @Test
    fun `bottom sheet opening is tracked`() {
        val userProfile = getDefaultUserProfile()
        viewModel.onBottomSheetOpen(userProfile, mock(), LIKE_NOTIFICATION_LIST)

        verify(analyticsUtilsWrapper, times(1)).trackUserProfileShown(anyString())
    }

    @Test
    fun `bottom sheet is closed on cancel`() {
        viewModel.onBottomSheetCancelled()

        requireNotNull(sheetAction).let {
            assertThat(it is HideBottomSheet).isTrue
        }
    }

    private fun getDefaultUserProfile(): UserProfile {
        return UserProfile(
                userAvatarUrl = "userAvatarUrl",
                blavatarUrl = "blavatarUrl",
                userName = "userName",
                userLogin = "userLogin",
                userBio = "userBio",
                siteTitle = "siteTitle",
                siteUrl = "siteUrl",
                siteId = 100L
        )
    }

    private fun setupObservers() {
        sheetUiState = null
        sheetAction = null

        viewModel.bottomSheetUiState.observeForever {
            sheetUiState = it
        }

        viewModel.onBottomSheetAction.observeForever {
            it.applyIfNotHandled {
                sheetAction = this
            }
        }
    }
}
