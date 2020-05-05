package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.VISIBILITY
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.PublishButtonUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

@RunWith(MockitoJUnitRunner::class)
class PrepublishingHomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingHomeViewModel
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var getPostTagsUseCase: GetPostTagsUseCase
    @Mock lateinit var site: SiteModel

    @Before
    fun setUp() {
        viewModel = PrepublishingHomeViewModel(getPostTagsUseCase, postSettingsUtils)
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn("")
        whenever(editPostRepository.getPost()).thenReturn(PostModel())
        whenever(site.name).thenReturn("")
    }

    @Test
    fun `verify that home actions are propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 3

        // act
        viewModel.start(mock(), site)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(HomeUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that header ui state is propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 1

        // act
        viewModel.start(mock(), site)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(HeaderUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that publish button ui state is propagated to uiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 1

        // act
        viewModel.start(mock(), site)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(PublishButtonUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that publish action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = PUBLISH

        // act
        viewModel.start(mock(), site)
        val publishAction = getHomeUiState(expectedActionType)
        publishAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that visibility action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        viewModel.start(mock(), site)
        val visibilityAction = getHomeUiState(expectedActionType)
        visibilityAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that tags action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = VISIBILITY

        // act
        viewModel.start(mock(), site)
        val tagsAction = getHomeUiState(expectedActionType)
        tagsAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that publish action result is propagated from postSettingsUtils`() {
        // arrange
        val expectedLabel = "test data"
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn(expectedLabel)

        // act
        viewModel.start(editPostRepository, site)
        val publishAction = getHomeUiState(PUBLISH)

        // assert
        assertThat((publishAction?.actionResult as? UiStringText)?.text).isEqualTo(expectedLabel)
    }

    @Test
    fun `verify that tags action result is propagated from getPostTagsUseCase`() {
        // arrange
        val expectedTags = "test, data"
        whenever(getPostTagsUseCase.getTags(editPostRepository)).thenReturn(expectedTags)

        // act
        viewModel.start(editPostRepository, site)
        val tagsAction = getHomeUiState(TAGS)

        // assert
        assertThat((tagsAction?.actionResult as? UiStringText)?.text).isEqualTo(expectedTags)
    }

    @Test
    fun `verify that tags not set is used when tags from getPostTagsUseCase is null`() {
        // arrange
        whenever(getPostTagsUseCase.getTags(editPostRepository)).thenReturn(null)

        // act
        viewModel.start(editPostRepository, site)
        val tagsAction = getHomeUiState(TAGS)

        // assert
        assertThat((tagsAction?.actionResult as? UiStringRes)?.stringRes)
                .isEqualTo(R.string.prepublishing_nudges_home_tags_not_set)
    }

    @Test
    fun `verify that header ui state's siteIconUrl is an empty string when site iconUrl is null`() {
        // arrange
        val expectedEmptyString = ""
        whenever(site.iconUrl).thenReturn(null)

        // act
        viewModel.start(editPostRepository, site)
        val headerUiState = getHeaderUiState()

        // assert
        assertThat((headerUiState?.siteIconUrl)).isEqualTo(expectedEmptyString)
    }

    @Test
    fun `verify that header ui state's siteIconUrl is set with site iconUrl`() {
        // arrange
        val expectedIconUrl = "/example/icon.png"
        whenever(site.iconUrl).thenReturn(expectedIconUrl)

        // act
        viewModel.start(editPostRepository, site)
        val headerUiState = getHeaderUiState()

        // assert
        assertThat((headerUiState?.siteIconUrl)).isEqualTo(expectedIconUrl)
    }

    @Test
    fun `verify that header ui state's siteName is set with site name`() {
        // arrange
        val expectedName = "Site Title"
        whenever(site.name).thenReturn(expectedName)

        // act
        viewModel.start(editPostRepository, site)
        val headerUiState = getHeaderUiState()

        // assert
        assertThat(headerUiState?.siteName?.text).isEqualTo(expectedName)
    }

    private fun getHeaderUiState() = viewModel.uiState.value?.filterIsInstance(HeaderUiState::class.java)?.first()

    private fun getHomeUiState(actionType: ActionType): HomeUiState? {
        val actions = viewModel.uiState.value
                ?.filterIsInstance(HomeUiState::class.java)
        return actions?.find { it.actionType == actionType }
    }
}
