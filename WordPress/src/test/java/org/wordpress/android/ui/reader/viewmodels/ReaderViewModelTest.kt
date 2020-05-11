package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState
import java.util.Date

private const val DUMMY_CURRENT_TIME: Long = 10000000000
@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderViewModel

    @Mock lateinit var appPrefsWrapper: AppPrefsWrapper
    @Mock lateinit var dateProvider: DateProvider
    @Mock lateinit var loadReaderTabsUseCase: LoadReaderTabsUseCase

    @Before
    fun setup() {
        viewModel = ReaderViewModel(TEST_DISPATCHER, appPrefsWrapper, dateProvider, loadReaderTabsUseCase)

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
    }

    @Test
    fun `updateTags invoked on first start`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(-1)
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `updateTags NOT invoked if lastUpdate within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD + 1)
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNull()
    }

    @Test
    fun `updateTags invoked if lastUpdate NOT within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD - 1)
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `UiState is NOT updated when loaded tags are empty`() = test {
        // Arrange
        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(ReaderTagList())
        // Act
        viewModel.start()
        // Assert
        assertThat(viewModel.uiState.value).isNull()
    }

    @Test
    fun `UiState is updated in start() when loaded tags are NOT empty`() = test {
        // Arrange
        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(createNonEmptyReaderTagList())
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        viewModel.start()
        // Assert
        assertThat(state).isNotNull
    }

    @Test
    fun `Tags are reloaded when FollowedTagsChanged event is received`() = test {
        // Arrange
        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(createNonEmptyReaderTagList())
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        viewModel.onTagsUpdated(mock())
        // Assert
        assertThat(state).isNotNull
    }

    private fun <T> testWithEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(ReaderTagList())
            block()
        }
    }

    private fun createNonEmptyReaderTagList(): ReaderTagList {
        return ReaderTagList().apply { add(mock()) }
    }
}
