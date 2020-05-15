package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType.FOLLOWED
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState
import org.wordpress.android.viewmodel.Event
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
    private val emptyReaderTagList = ReaderTagList()
    private val nonEmptyReaderTagList = ReaderTagList().apply {
        this.add(mock())
        this.add(mock())
        this.add(mock())
        this.add(mock())
    }

    @Before
    fun setup() {
        viewModel = ReaderViewModel(
                TEST_DISPATCHER,
                TEST_DISPATCHER,
                appPrefsWrapper,
                dateProvider,
                loadReaderTabsUseCase
        )

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(null)
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
    fun `UiState is updated in start() when loaded tags are NOT empty`() = testWithNonEmptyTags {
        // Arrange
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
    fun `Tags are reloaded when FollowedTagsChanged event is received`() = testWithNonEmptyTags {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        viewModel.onTagsUpdated(mock())
        // Assert
        assertThat(state).isNotNull
    }

    @Test
    fun `Last selected tab is stored into shared preferences`() {
        // Arrange
        val selectedTag: ReaderTag = mock()
        // Act
        viewModel.onTagChanged(selectedTag)
        // Assert
        verify(appPrefsWrapper).setReaderTag(any())
    }

    @Test
    fun `Last selected tab is restored after restart`() = testWithNonEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(nonEmptyReaderTagList[3])

        var tabPosition: TabPosition? = null
        viewModel.selectTab.observeForever {
            tabPosition = it.getContentIfNotHandled()
        }
        // Act
        viewModel.start()
        // Assert
        assertThat(tabPosition).isEqualTo(3)
    }

    @Test
    fun `SelectTab not invoked when last selected tab is null`() = testWithNonEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(null)

        var tabPosition: TabPosition? = null
        viewModel.selectTab.observeForever {
            tabPosition = it.getContentIfNotHandled()
        }
        // Act
        viewModel.start()
        // Assert
        assertThat(tabPosition).isNull()
    }

    @Test
    fun `Position is changed when selectedTabChange`() = test {
        // Arrange
        val tagList = createNonMockedNonEmptyReaderTagList()
        val readerTag = tagList[2]

        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(tagList)

        viewModel.uiState.observeForever { }

        var tabPosition: TabPosition? = null
            viewModel.selectTab.observeForever {
                tabPosition = it.getContentIfNotHandled()
        }

        // Act
        viewModel.start()
        viewModel.selectedTabChange(readerTag)

        // Assert
        assertThat(tabPosition).isEqualTo(2)
    }

    @Test
    fun `OnSearchActionClicked emits showSearch event`() {
        // Arrange
        var event: Event<Unit>? = null
        viewModel.showSearch.observeForever {
            event = it
        }
        // Act
        viewModel.onSearchActionClicked()

        // Assert
        assertThat(event).isNotNull
    }

    private fun <T> testWithEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(emptyReaderTagList)
            block()
        }
    }

    private fun <T> testWithNonEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(nonEmptyReaderTagList)
            block()
        }
    }

    private fun createNonMockedNonEmptyReaderTagList(): ReaderTagList {
        return ReaderTagList().apply {
            add(ReaderTag("Following", "Following", "Following", " ", FOLLOWED))
            add(ReaderTag("Discover", "Discover", "Discover", " ", FOLLOWED))
            add(ReaderTag("Like", "Like", "Like", " ", FOLLOWED))
            add(ReaderTag("Saved", "Saved", "Saved", "Saved", FOLLOWED))
        }
    }
}
