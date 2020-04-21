package org.wordpress.android.ui.posts.prepublishing

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.posts.GetPostTagsUseCase
import org.wordpress.android.ui.posts.PrepublishingTagsViewModel
import org.wordpress.android.ui.posts.UpdatePostTagsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class PrepublishingTagsViewModelTest: BaseUnitTest() {
    private lateinit var viewModel: PrepublishingTagsViewModel
    @Mock lateinit var getPostTagsUseCase: GetPostTagsUseCase
    @Mock lateinit var updatePostTagsUseCase: UpdatePostTagsUseCase

    @Before
    fun setup() {
        viewModel = PrepublishingTagsViewModel(getPostTagsUseCase, updatePostTagsUseCase, TEST_DISPATCHER)
    }

    @Test
    fun `when viewModel is started updateToolbarTitle is called with the tags title`() {
        var title: UiStringRes? = null
        viewModel.updateToolbarTitle.observeForever {
            title = it as UiStringRes
        }

        viewModel.start(mock())

        Assertions.assertThat(title?.stringRes).isEqualTo(R.string.prepublishing_nudges_toolbar_title_tags)
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever {
            event = it
        }

        viewModel.onBackButtonClicked()

        Assertions.assertThat(event).isNotNull
    }

    @Test
    fun `when onCloseClicked is triggered dismissBottomSheet is called`() {
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        viewModel.onCloseButtonClicked()

        Assertions.assertThat(event).isNotNull
    }
}
