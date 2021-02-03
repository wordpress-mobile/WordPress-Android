package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.MainActionListItem.ActionType
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_POST
import org.wordpress.android.ui.main.MainActionListItem.ActionType.CREATE_NEW_STORY
import org.wordpress.android.ui.main.MainActionListItem.CreateAction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.WPStoriesFeatureConfig

class PostListCreateMenuViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PostListCreateMenuViewModel
    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper
    @Mock lateinit var wpStoriesFeatureConfig: WPStoriesFeatureConfig
    @Mock lateinit var site: SiteModel

    @Before
    fun setUp() {
        viewModel = PostListCreateMenuViewModel(appPrefsWrapper, analyticsTrackerWrapper, wpStoriesFeatureConfig)
    }

    @Test
    fun `bottom sheet action is new post when new post is tapped`() {
        viewModel.start(site)
        val action = getCreateAction(CREATE_NEW_POST)
        action.onClickAction?.invoke(CREATE_NEW_POST)

        Assertions.assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_POST)
    }

    @Test
    fun `bottom sheet action is new story when new story is tapped`() {
        viewModel.start(site)
        val action = getCreateAction(CREATE_NEW_STORY)
        action.onClickAction?.invoke(CREATE_NEW_STORY)

        Assertions.assertThat(viewModel.createAction.value).isEqualTo(CREATE_NEW_STORY)
    }

    @Test
    fun `bottom sheet showing is triggered with false once an action is tapped`() {
        viewModel.start(site)
        val action = getCreateAction(CREATE_NEW_POST)
        action.onClickAction?.invoke(CREATE_NEW_POST)

        Assertions.assertThat(viewModel.isBottomSheetShowing.value?.getContentIfNotHandled()).isFalse()
    }

    @Test
    fun `when onFabClicked then bottom sheet showing is true`() {
        viewModel.onFabClicked()

        Assertions.assertThat(viewModel.isBottomSheetShowing.value?.getContentIfNotHandled()).isTrue()
    }

    @Test
    fun `when onFabClicked then appPrefsWrapper's setPostListFabTooltipDisabled is called with true`() {
        viewModel.onFabClicked()

        verify(appPrefsWrapper).setPostListFabTooltipDisabled(eq(true))
    }

    @Test
    fun `if appPrefsWrapper's isPostListFabTooltipDisabled is false then isFabTooltipVisible is true`() {
        whenever(appPrefsWrapper.isPostListFabTooltipDisabled()).thenReturn(false)
        viewModel.start(site)

        Assertions.assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isTrue()
    }

    @Test
    fun `if appPrefsWrapper's isPostListFabTooltipDisabled is true then isFabTooltipVisible is false`() {
        whenever(appPrefsWrapper.isPostListFabTooltipDisabled()).thenReturn(true)
        viewModel.start(site)

        Assertions.assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `when tooltipTapped then setPostListFabTooltipDisabled is called with true`() {
        viewModel.onTooltipTapped()

        verify(appPrefsWrapper).setPostListFabTooltipDisabled(eq(true))
    }

    @Test
    fun `when tooltipTapped then isFabTooltipVisible is false`() {
        viewModel.start(site)
        viewModel.onTooltipTapped()

        Assertions.assertThat(viewModel.fabUiState.value?.isFabTooltipVisible).isFalse()
    }

    @Test
    fun `start set expected content message if stories not enabled`() {
        setupWPStoriesFeatureConfigEnabled(false)

        viewModel.start(site)
        Assertions.assertThat(viewModel.fabUiState.value!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_fab_tooltip)
    }

    @Test
    fun `start set expected content message if stories enabled`() {
        setupWPStoriesFeatureConfigEnabled(true)
        whenever(site.isWPCom).thenReturn(true)

        viewModel.start(site)
        Assertions.assertThat(viewModel.fabUiState.value!!.CreateContentMessageId)
                .isEqualTo(R.string.create_post_story_fab_tooltip)
    }

    private fun getCreateAction(actionType: ActionType) =
            viewModel.mainActions.value?.first { it.actionType == actionType } as CreateAction

    private fun setupWPStoriesFeatureConfigEnabled(buildConfigValue: Boolean) {
        whenever(wpStoriesFeatureConfig.isEnabled()).thenReturn(buildConfigValue)
    }
}
