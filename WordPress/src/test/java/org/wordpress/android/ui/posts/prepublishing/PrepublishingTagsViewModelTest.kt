package org.wordpress.android.ui.posts.prepublishing

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doNothing
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.test
import org.wordpress.android.ui.posts.PrepublishingTagsViewModel
import org.wordpress.android.ui.posts.UpdatePostTagsUseCase
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event

@InternalCoroutinesApi
class PrepublishingTagsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrepublishingTagsViewModel
    @Mock lateinit var updatePostTagsUseCase: UpdatePostTagsUseCase

    @Before
    fun setup() {
        viewModel = PrepublishingTagsViewModel(mock(), updatePostTagsUseCase, mock(), TEST_DISPATCHER)
    }

    @Test
    fun `when viewModel is started updateToolbarTitle is called with the tags title`() {
        var title: UiStringRes? = null
        viewModel.toolbarTitleUiState.observeForever {
            title = it as UiStringRes
        }

        viewModel.start(mock())

        assertThat(title?.stringRes).isEqualTo(R.string.prepublishing_nudges_toolbar_title_tags)
    }

    @Test
    fun `when onBackClicked is triggered navigateToHomeScreen is called`() {
        var event: Event<Unit>? = null
        viewModel.navigateToHomeScreen.observeForever {
            event = it
        }

        viewModel.onBackButtonClicked()

        assertThat(event).isNotNull
    }

    @Test
    fun `when onCloseClicked is triggered dismissBottomSheet is called`() {
        var event: Event<Unit>? = null
        viewModel.dismissBottomSheet.observeForever {
            event = it
        }

        viewModel.onCloseButtonClicked()

        assertThat(event).isNotNull
    }

    @Test
    fun `when onTagsSelected is called updatePostTagsUseCase's updateTags should be called`() = test {
        val expectedTags = "test, data"
        val captor = ArgumentCaptor.forClass(String::class.java)
        doNothing().whenever(updatePostTagsUseCase).updateTags(captor.capture(), any())

        viewModel.start(mock())
        viewModel.onTagsSelected(expectedTags)

        assertThat(captor.value).isEqualTo(expectedTags)
    }
}
