package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.PUBLISH
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ActionType.TAGS
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.ButtonUiState.PublishButtonUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HeaderUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.HomeUiState
import org.wordpress.android.ui.posts.PrepublishingHomeItemUiState.StoryTitleUiState
import org.wordpress.android.ui.posts.prepublishing.home.usecases.GetButtonUiStateUseCase
import org.wordpress.android.ui.stories.StoryRepositoryWrapper
import org.wordpress.android.ui.stories.usecase.UpdateStoryPostTitleUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event

class PrepublishingHomeViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingHomeViewModel
    @Mock lateinit var postSettingsUtils: PostSettingsUtils
    @Mock lateinit var editPostRepository: EditPostRepository
    @Mock lateinit var getPostTagsUseCase: GetPostTagsUseCase
    @Mock lateinit var getButtonUiStateUseCase: GetButtonUiStateUseCase
    @Mock lateinit var storyRepositoryWrapper: StoryRepositoryWrapper
    @Mock lateinit var updateStoryTitleUseCase: UpdateStoryPostTitleUseCase
    @Mock lateinit var site: SiteModel

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = PrepublishingHomeViewModel(
                getPostTagsUseCase,
                postSettingsUtils,
                getButtonUiStateUseCase,
                mock(),
                storyRepositoryWrapper,
                updateStoryTitleUseCase,
                TEST_DISPATCHER
        )
        whenever(
                getButtonUiStateUseCase.getUiState(
                        any(),
                        any(),
                        any()
                )
        ).doAnswer {
            PublishButtonUiState(it.arguments[2] as (PublishPost) -> Unit)
        }
        whenever(editPostRepository.getEditablePost()).thenReturn(PostModel())
        whenever(editPostRepository.title).thenReturn("")
        whenever(postSettingsUtils.getPublishDateLabel(any())).thenReturn((""))
        whenever(site.name).thenReturn("")
        whenever(storyRepositoryWrapper.getCurrentStoryThumbnailUrl()).thenReturn("")
    }

    @Test
    fun `verify that post home actions are propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 2

        // act
        viewModel.start(mock(), site, false)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(HomeUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that page home actions are propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 1
        whenever(editPostRepository.isPage).thenReturn(true)

        // act
        viewModel.start(editPostRepository, site, false)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(HomeUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that tags actions is propagated to prepublishingHomeUiState once post is not a page`() {
        // arrange
        whenever(editPostRepository.isPage).thenReturn(false)

        // act
        viewModel.start(editPostRepository, site, false)

        // assert
        assertThat(getHomeUiState(TAGS)).isNotNull()
    }

    @Test
    fun `verify that tags actions is not propagated to prepublishingHomeUiState once post is a page`() {
        // arrange
        whenever(editPostRepository.isPage).thenReturn(true)

        // act
        viewModel.start(editPostRepository, site, false)

        // assert
        assertThat(getHomeUiState(TAGS)).isNull()
    }

    @Test
    fun `verify that header ui state is propagated to prepublishingHomeUiState once the viewModel is started`() {
        // arrange
        val expectedActionsAmount = 1

        // act
        viewModel.start(mock(), site, false)

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
        viewModel.start(mock(), site, false)

        // assert
        assertThat(viewModel.uiState.value?.filterIsInstance(ButtonUiState::class.java)?.size).isEqualTo(
                expectedActionsAmount
        )
    }

    @Test
    fun `verify that publish action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = PUBLISH

        // act
        viewModel.start(mock(), site, false)
        val publishAction = getHomeUiState(expectedActionType)
        publishAction?.onActionClicked?.invoke(expectedActionType)

        // assert
        assertThat(requireNotNull(viewModel.onActionClicked.value).peekContent()).isEqualTo(expectedActionType)
    }

    @Test
    fun `verify that tags action type is propagated to prepublishingActionType`() {
        // arrange
        val expectedActionType = TAGS

        // act
        viewModel.start(mock(), site, false)
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
        viewModel.start(editPostRepository, site, false)
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
        viewModel.start(editPostRepository, site, false)
        val tagsAction = getHomeUiState(TAGS)

        // assert
        assertThat((tagsAction?.actionResult as? UiStringText)?.text).isEqualTo(expectedTags)
    }

    @Test
    fun `verify that tags not set is used when tags from getPostTagsUseCase is null`() {
        // arrange
        whenever(getPostTagsUseCase.getTags(editPostRepository)).thenReturn(null)

        // act
        viewModel.start(editPostRepository, site, false)
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
        viewModel.start(editPostRepository, site, false)
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
        viewModel.start(editPostRepository, site, false)
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
        viewModel.start(editPostRepository, site, false)
        val headerUiState = getHeaderUiState()

        // assert
        assertThat(headerUiState?.siteName?.text).isEqualTo(expectedName)
    }

    @Test
    fun `verify that tapping submit button will invoke onSubmitButtonClicked`() {
        // arrange
        var event: Event<PublishPost>? = null
        viewModel.onSubmitButtonClicked.observeForever {
            event = it
        }

        // act
        viewModel.start(editPostRepository, site, false)
        val buttonUiState = getButtonUiState()
        buttonUiState?.onButtonClicked?.invoke(true)

        // assert
        assertThat(event).isNotNull
    }

    @Test
    fun `verify that PUBLISH action is unclickable if PostStatus is PRIVATE`() {
        whenever(editPostRepository.status).thenReturn(PRIVATE)

        viewModel.start(editPostRepository, site, false)

        val uiState = getHomeUiState(PUBLISH)

        assertThat(uiState?.actionClickable).isFalse()
    }

    @Test
    fun `verify that TAGS action is clickable if PostStatus is PRIVATE`() {
        whenever(editPostRepository.status).thenReturn(PRIVATE)

        viewModel.start(editPostRepository, site, false)

        val uiState = getHomeUiState(TAGS)

        assertThat(uiState?.actionClickable).isTrue()
    }

    @Test
    fun `verify that if isStoryPost is true then StoryTitleUiState is created`() {
        val expectedIsStoryPost = true

        viewModel.start(editPostRepository, site, expectedIsStoryPost)

        assertThat(getStoryTitleUiStateList()).isNotEmpty
    }

    @Test
    fun `verify that if isStoryPost is false then StoryTitleUiState is not created`() {
        val expectedIsStoryPost = false

        viewModel.start(editPostRepository, site, expectedIsStoryPost)

        assertThat(getStoryTitleUiStateList()).isEmpty()
    }

    @Test
    fun `verify that if storyThumbnailUrl is set to StoryTitleUiState`() {
        val storyThumbnailUrl = "/example.png"
        whenever(storyRepositoryWrapper.getCurrentStoryThumbnailUrl()).thenReturn(storyThumbnailUrl)

        viewModel.start(editPostRepository, site, true)

        assertThat(getStoryTitleUiState()?.storyThumbnailUrl).isEqualTo(storyThumbnailUrl)
    }

    @Test
    fun `verify that if post title is set then storyTitle text shouldn't be empty`() {
        val storyTitle = "Story Title"
        whenever(editPostRepository.title).thenReturn(storyTitle)

        viewModel.start(editPostRepository, site, true)

        assertThat(getStoryTitleUiState()?.storyTitle?.text).isEqualTo(storyTitle)
    }

    @Test
    fun `verify that if post title is null then storyTitle text should be empty`() {
        whenever(editPostRepository.title).thenReturn(null)

        viewModel.start(editPostRepository, site, true)

        assertThat(getStoryTitleUiState()?.storyTitle?.text).isEmpty()
    }

    @Test
    fun `verify that if storyTitleChanged then setCurrentStoryTitle is called`() {
        val storyTitle = "Story Title"

        viewModel.start(editPostRepository, site, true)
        getStoryTitleUiState()?.onStoryTitleChanged?.invoke(storyTitle)

        verify(storyRepositoryWrapper).setCurrentStoryTitle(eq(storyTitle))
    }

    @Test
    fun `verify that if storyTitleChanged then updateStoryPostTitleUseCase is called`() {
        val storyTitle = "Story Title"

        viewModel.start(editPostRepository, site, true)
        getStoryTitleUiState()?.onStoryTitleChanged?.invoke(storyTitle)

        verify(updateStoryTitleUseCase).updateStoryTitle(eq(storyTitle), any())
    }

    private fun getHeaderUiState() = viewModel.uiState.value?.filterIsInstance(HeaderUiState::class.java)?.first()
    private fun getStoryTitleUiState() = getStoryTitleUiStateList()?.first()
    private fun getStoryTitleUiStateList() =
            viewModel.uiState.value?.filterIsInstance(StoryTitleUiState::class.java)

    private fun getButtonUiState(): ButtonUiState? {
        return viewModel.uiState.value?.filterIsInstance(ButtonUiState::class.java)?.first()
    }

    private fun getHomeUiState(actionType: ActionType): HomeUiState? {
        val actions = viewModel.uiState.value
                ?.filterIsInstance(HomeUiState::class.java)
        return actions?.find { it.actionType == actionType }
    }
}
