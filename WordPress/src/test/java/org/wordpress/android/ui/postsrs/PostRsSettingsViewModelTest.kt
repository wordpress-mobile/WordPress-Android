package org.wordpress.android.ui.postsrs

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.postsrs.PostRsSettingsViewModel.Companion.EXTRA_POST_ID
import org.wordpress.android.ui.postsrs.data.PostRsRestClient
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import uniffi.wp_api.PostFormat
import uniffi.wp_api.PostStatus
import java.util.Calendar

@ExperimentalCoroutinesApi
@Suppress("LargeClass")
class PostRsSettingsViewModelTest :
    BaseUnitTest(StandardTestDispatcher()) {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var wpApiClientProvider: WpApiClientProvider

    @Mock
    lateinit var restClient: PostRsRestClient

    @Mock
    lateinit var resourceProvider: ResourceProvider

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    lateinit var uriToFileMapper: UriToFileMapper

    private lateinit var site: SiteModel
    private var activeViewModel: PostRsSettingsViewModel? =
        null

    @Before
    fun setUp() {
        site = SiteModel().apply {
            id = 1
            siteId = 123L
        }
        whenever(selectedSiteRepository.getSelectedSite())
            .thenReturn(site)
        whenever(resourceProvider.getString(any()))
            .thenReturn("error")
        // Default to offline so loadPost() in init exits
        // early without launching a coroutine that would
        // fail against un-mocked API providers.
        whenever(
            networkUtilsWrapper.isNetworkAvailable()
        ).thenReturn(false)
    }

    @After
    fun tearDown() {
        activeViewModel?.viewModelScope?.cancel()
        activeViewModel = null
    }

    private fun createViewModel(
        postId: Long = TEST_POST_ID,
        hasSite: Boolean = true,
    ): PostRsSettingsViewModel {
        if (!hasSite) {
            whenever(
                selectedSiteRepository.getSelectedSite()
            ).thenReturn(null)
        }
        val handle = SavedStateHandle(
            mapOf(EXTRA_POST_ID to postId)
        )
        return PostRsSettingsViewModel(
            savedStateHandle = handle,
            selectedSiteRepository =
                selectedSiteRepository,
            wpApiClientProvider = wpApiClientProvider,
            restClient = restClient,
            resourceProvider = resourceProvider,
            networkUtilsWrapper = networkUtilsWrapper,
            uriToFileMapper = uriToFileMapper,
        ).also { activeViewModel = it }
    }

    // region Init

    @Test
    fun `when no site, emits Finish event`() = test {
        val viewModel = createViewModel(hasSite = false)

        viewModel.events.test {
            val event = awaitItem()
            assertThat(event)
                .isEqualTo(PostRsSettingsEvent.Finish)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `when no site, does not emit snackbar`() = test {
        val viewModel = createViewModel(hasSite = false)

        viewModel.snackbarMessages.test {
            expectNoEvents()
        }
    }

    @Test
    fun `when site exists and online, sets isLoading`() {
        whenever(
            networkUtilsWrapper.isNetworkAvailable()
        ).thenReturn(true)
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.isLoading)
            .isTrue()
    }

    @Test
    fun `when site exists and offline, shows error`() {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.isLoading)
            .isFalse()
        assertThat(viewModel.uiState.value.error)
            .isNotNull()
    }

    @Test
    fun `retry while offline shows error`() {
        val viewModel = createViewModel()

        viewModel.retry()

        assertThat(viewModel.uiState.value.isLoading)
            .isFalse()
        assertThat(viewModel.uiState.value.error)
            .isNotNull()
    }

    // endregion

    // region Edit methods

    @Test
    fun `onStatusSelected updates editedStatus`() {
        val viewModel = createViewModel()

        viewModel.onStatusSelected(PostStatus.Draft)

        assertThat(
            viewModel.uiState.value.editedStatus
        ).isEqualTo(PostStatus.Draft)
    }

    @Test
    fun `onStatusSelected keeps edit when original is null`() {
        val viewModel = createViewModel()
        // initial postStatus is null, select then re-select
        viewModel.onStatusSelected(PostStatus.Draft)
        assertThat(
            viewModel.uiState.value.editedStatus
        ).isNotNull()

        // Re-selecting same value keeps edit because the
        // original is null (Draft != null).
        viewModel.onStatusSelected(PostStatus.Draft)
        assertThat(
            viewModel.uiState.value.editedStatus
        ).isEqualTo(PostStatus.Draft)
    }

    @Test
    fun `onPasswordSet updates editedPassword`() {
        val viewModel = createViewModel()

        viewModel.onPasswordSet("secret")

        assertThat(
            viewModel.uiState.value.editedPassword
        ).isEqualTo("secret")
    }

    @Test
    fun `onPasswordSet clears when same as original`() {
        val viewModel = createViewModel()
        // original password is null, "" != null => edited
        viewModel.onPasswordSet("")

        // password is null originally, empty string check:
        // original = null ?: "" -> "", "" == "" -> null
        assertThat(
            viewModel.uiState.value.editedPassword
        ).isNull()
    }

    @Test
    fun `onStickyToggled flips sticky`() {
        val viewModel = createViewModel()

        viewModel.onStickyToggled()

        assertThat(
            viewModel.uiState.value.editedSticky
        ).isTrue()
    }

    @Test
    fun `onStickyToggled twice clears edit`() {
        val viewModel = createViewModel()

        viewModel.onStickyToggled()
        viewModel.onStickyToggled()

        assertThat(
            viewModel.uiState.value.editedSticky
        ).isNull()
    }

    @Test
    fun `onSlugSet updates editedSlug`() {
        val viewModel = createViewModel()

        viewModel.onSlugSet("new-slug")

        assertThat(
            viewModel.uiState.value.editedSlug
        ).isEqualTo("new-slug")
    }

    @Test
    fun `onSlugSet clears when same as original`() {
        val viewModel = createViewModel()
        // original slug is ""
        viewModel.onSlugSet("")

        assertThat(
            viewModel.uiState.value.editedSlug
        ).isNull()
    }

    @Test
    fun `onExcerptSet updates editedExcerpt`() {
        val viewModel = createViewModel()

        viewModel.onExcerptSet("new excerpt")

        assertThat(
            viewModel.uiState.value.editedExcerpt
        ).isEqualTo("new excerpt")
    }

    @Test
    fun `onExcerptSet clears when same as original`() {
        val viewModel = createViewModel()

        viewModel.onExcerptSet("")

        assertThat(
            viewModel.uiState.value.editedExcerpt
        ).isNull()
    }

    @Test
    fun `onFormatSelected updates editedFormat`() {
        val viewModel = createViewModel()

        viewModel.onFormatSelected(PostFormat.Gallery)

        assertThat(
            viewModel.uiState.value.editedFormat
        ).isEqualTo(PostFormat.Gallery)
    }

    @Test
    fun `onFeaturedImageRemoved sets editedFeaturedImageId`() {
        val viewModel = createViewModel()
        // original featuredImageId is 0, so removing
        // (setting to 0) when already 0 => null edit
        viewModel.onFeaturedImageRemoved()

        assertThat(
            viewModel.uiState.value.editedFeaturedImageId
        ).isNull()
    }

    // endregion

    // region Dialog state

    @Test
    fun `onStatusClicked sets StatusDialog`() {
        val viewModel = createViewModel()

        viewModel.onStatusClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.StatusDialog)
    }

    @Test
    fun `onPasswordClicked sets PasswordDialog`() {
        val viewModel = createViewModel()

        viewModel.onPasswordClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.PasswordDialog)
    }

    @Test
    fun `onSlugClicked sets SlugDialog`() {
        val viewModel = createViewModel()

        viewModel.onSlugClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.SlugDialog)
    }

    @Test
    fun `onExcerptClicked sets ExcerptDialog`() {
        val viewModel = createViewModel()

        viewModel.onExcerptClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.ExcerptDialog)
    }

    @Test
    fun `onFormatClicked sets FormatDialog`() {
        val viewModel = createViewModel()

        viewModel.onFormatClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.FormatDialog)
    }

    @Test
    fun `onDateClicked sets DateDialog`() {
        val viewModel = createViewModel()

        viewModel.onDateClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.DateDialog)
    }

    @Test
    fun `onDismissDialog resets dialogState to None`() {
        val viewModel = createViewModel()
        viewModel.onStatusClicked()

        viewModel.onDismissDialog()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.None)
    }

    @Test
    fun `onDismissDialog clears author search state`() {
        val viewModel = createViewModel()

        viewModel.onDismissDialog()

        val state = viewModel.uiState.value
        assertThat(state.authorSearchQuery).isEmpty()
        assertThat(state.isSearchingAuthors).isFalse()
        assertThat(state.siteAuthors).isEmpty()
        assertThat(state.canLoadMoreAuthors).isFalse()
    }

    // endregion

    // region hasChanges

    @Test
    fun `hasChanges is false initially`() {
        val viewModel = createViewModel()
        assertThat(viewModel.uiState.value.hasChanges)
            .isFalse()
    }

    @Test
    fun `hasChanges is true after status edit`() {
        val viewModel = createViewModel()

        viewModel.onStatusSelected(PostStatus.Pending)

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    @Test
    fun `hasChanges is true after password edit`() {
        val viewModel = createViewModel()

        viewModel.onPasswordSet("abc")

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    @Test
    fun `hasChanges is true after slug edit`() {
        val viewModel = createViewModel()

        viewModel.onSlugSet("new-slug")

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    @Test
    fun `hasChanges is true after sticky toggle`() {
        val viewModel = createViewModel()

        viewModel.onStickyToggled()

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    @Test
    fun `hasChanges is true after excerpt edit`() {
        val viewModel = createViewModel()

        viewModel.onExcerptSet("excerpt text")

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    @Test
    fun `hasChanges is true after format edit`() {
        val viewModel = createViewModel()

        viewModel.onFormatSelected(PostFormat.Video)

        assertThat(viewModel.uiState.value.hasChanges)
            .isTrue()
    }

    // endregion

    // region Save

    @Test
    fun `onSaveClicked with no changes is no-op`() =
        test {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onSaveClicked()
                expectNoEvents()
            }
        }

    @Test
    fun `onSaveClicked with changes sets isSaving`() {
        val viewModel = createViewModel()
        viewModel.onStatusSelected(PostStatus.Draft)

        viewModel.onSaveClicked()

        assertThat(viewModel.uiState.value.isSaving)
            .isTrue()
    }

    @Test
    fun `onSaveClicked while already saving is no-op`() {
        val viewModel = createViewModel()
        viewModel.onStatusSelected(PostStatus.Draft)
        viewModel.onSaveClicked()
        assertThat(viewModel.uiState.value.isSaving)
            .isTrue()

        // Second save attempt should be no-op
        viewModel.onSaveClicked()

        assertThat(viewModel.uiState.value.isSaving)
            .isTrue()
    }

    // endregion

    // region Back

    @Test
    fun `onBackClicked with no changes emits Finish`() =
        test {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onBackClicked()

                val event = awaitItem()
                assertThat(event)
                    .isEqualTo(PostRsSettingsEvent.Finish)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onBackClicked with changes shows DiscardDialog`() {
        val viewModel = createViewModel()
        viewModel.onStatusSelected(PostStatus.Draft)

        viewModel.onBackClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.DiscardDialog)
    }

    @Test
    fun `onDiscardConfirmed resets dialog and emits Finish`() =
        test {
            val viewModel = createViewModel()
            viewModel.onStatusSelected(PostStatus.Draft)
            viewModel.onBackClicked()

            viewModel.events.test {
                viewModel.onDiscardConfirmed()

                assertThat(
                    viewModel.uiState.value.dialogState
                ).isEqualTo(DialogState.None)

                val event = awaitItem()
                assertThat(event)
                    .isEqualTo(PostRsSettingsEvent.Finish)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    // region Refresh

    @Test
    fun `refreshPost sets isRefreshing true`() {
        val viewModel = createViewModel()
        whenever(
            networkUtilsWrapper.isNetworkAvailable()
        ).thenReturn(true)

        viewModel.refreshPost()

        assertThat(
            viewModel.uiState.value.isRefreshing
        ).isTrue()
    }

    @Test
    fun `refreshPost offline sends snackbar`() = test {
        val viewModel = createViewModel()

        viewModel.snackbarMessages.test {
            viewModel.refreshPost()

            val msg = awaitItem()
            assertThat(msg.message).isNotEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refreshPost offline does not set isRefreshing`() {
        val viewModel = createViewModel()

        viewModel.refreshPost()

        assertThat(
            viewModel.uiState.value.isRefreshing
        ).isFalse()
    }

    // endregion

    // region Author permission

    @Test
    fun `onAuthorClicked without permission sends snackbar`() =
        test {
            val viewModel = createViewModel()

            viewModel.snackbarMessages.test {
                viewModel.onAuthorClicked()

                val msg = awaitItem()
                assertThat(msg.message).isNotEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onAuthorClicked without permission keeps dialog None`() {
        val viewModel = createViewModel()

        viewModel.onAuthorClicked()

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.None)
    }

    // endregion

    // region Date and Time selection

    @Test
    fun `onDateSelected updates editedDate`() {
        val viewModel = createViewModel()

        viewModel.onDateSelected(2025, 5, 15)

        val date = viewModel.uiState.value.editedDate
        assertThat(date).isNotNull()
        val cal = Calendar.getInstance().apply {
            time = date!!
        }
        assertThat(cal[Calendar.YEAR]).isEqualTo(2025)
        assertThat(cal[Calendar.MONTH]).isEqualTo(5)
        assertThat(cal[Calendar.DAY_OF_MONTH])
            .isEqualTo(15)
    }

    @Test
    fun `onDateSelected transitions to TimeDialog`() {
        val viewModel = createViewModel()

        viewModel.onDateSelected(2025, 5, 15)

        assertThat(viewModel.uiState.value.dialogState)
            .isEqualTo(DialogState.TimeDialog)
    }

    @Test
    fun `onTimeSelected updates editedDate`() {
        val viewModel = createViewModel()

        viewModel.onTimeSelected(14, 30)

        val date = viewModel.uiState.value.editedDate
        assertThat(date).isNotNull()
        val cal = Calendar.getInstance().apply {
            time = date!!
        }
        assertThat(cal[Calendar.HOUR_OF_DAY])
            .isEqualTo(14)
        assertThat(cal[Calendar.MINUTE]).isEqualTo(30)
    }

    // endregion

    // region Author selection

    @Test
    fun `onAuthorSelected updates editedAuthor`() {
        val viewModel = createViewModel()

        viewModel.onAuthorSelected(99L)

        assertThat(
            viewModel.uiState.value.editedAuthor
        ).isEqualTo(99L)
    }

    @Test
    fun `onAuthorSelected with same as original clears edit`() {
        val viewModel = createViewModel()
        // authorId defaults to 0

        viewModel.onAuthorSelected(0L)

        assertThat(
            viewModel.uiState.value.editedAuthor
        ).isNull()
    }

    // endregion

    // region Categories and Tags

    @Test
    fun `onCategoriesClicked emits LaunchCategorySelection`() =
        test {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onCategoriesClicked()

                val event = awaitItem()
                assertThat(event).isInstanceOf(
                    PostRsSettingsEvent
                        .LaunchCategorySelection::class.java
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onCategoriesSelected with new ids updates edit`() {
        val viewModel = createViewModel()

        viewModel.onCategoriesSelected(
            longArrayOf(1L, 2L)
        )

        assertThat(
            viewModel.uiState.value.editedCategoryIds
        ).containsExactly(1L, 2L)
    }

    @Test
    fun `onTagsSelected with new ids updates edit`() {
        val viewModel = createViewModel()

        viewModel.onTagsSelected(longArrayOf(3L, 4L))

        assertThat(
            viewModel.uiState.value.editedTagIds
        ).containsExactly(3L, 4L)
    }

    @Test
    fun `onCategoriesSelected with same ids clears edit`() {
        val viewModel = createViewModel()
        // original categoryIds is empty list

        viewModel.onCategoriesSelected(longArrayOf())

        assertThat(
            viewModel.uiState.value.editedCategoryIds
        ).isNull()
    }

    // endregion

    // region Featured image

    @Test
    fun `onFeaturedImageSelected updates editedId`() {
        val viewModel = createViewModel()

        viewModel.onFeaturedImageSelected(99L)

        assertThat(
            viewModel.uiState.value.editedFeaturedImageId
        ).isEqualTo(99L)
    }

    @Test
    fun `onFeaturedImageSelected with same id is no-op`() {
        val viewModel = createViewModel()
        // effectiveFeaturedImageId defaults to 0

        viewModel.onFeaturedImageSelected(0L)

        assertThat(
            viewModel.uiState.value.editedFeaturedImageId
        ).isNull()
        assertThat(
            viewModel.uiState.value.featuredImage
        ).isEqualTo(FieldState.Empty)
    }

    @Test
    fun `onChooseFromWpMedia emits LaunchWpMediaPicker`() =
        test {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onChooseFromWpMedia()

                val event = awaitItem()
                assertThat(event).isEqualTo(
                    PostRsSettingsEvent.LaunchWpMediaPicker
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `onChooseFromDevice emits LaunchDeviceMediaPicker`() =
        test {
            val viewModel = createViewModel()

            viewModel.events.test {
                viewModel.onChooseFromDevice()

                val event = awaitItem()
                assertThat(event).isEqualTo(
                    PostRsSettingsEvent
                        .LaunchDeviceMediaPicker
                )
                cancelAndIgnoreRemainingEvents()
            }
        }

    // endregion

    companion object {
        private const val TEST_POST_ID = 42L
    }
}
