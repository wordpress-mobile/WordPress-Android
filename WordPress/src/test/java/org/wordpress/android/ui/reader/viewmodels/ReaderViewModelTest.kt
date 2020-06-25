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
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTag.DISCOVER_PATH
import org.wordpress.android.models.ReaderTag.FOLLOWING_PATH
import org.wordpress.android.models.ReaderTag.LIKED_PATH
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.test
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderViewModel.ReaderUiState.ContentUiState
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
    @Mock lateinit var readerTracker: ReaderTracker
    @Mock lateinit var accountStore: AccountStore

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
                loadReaderTabsUseCase,
                readerTracker,
                accountStore
        )

        whenever(dateProvider.getCurrentDate()).thenReturn(Date(DUMMY_CURRENT_TIME))
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(null)
        whenever(appPrefsWrapper.isReaderImprovementsPhase2Enabled()).thenReturn(false)
    }

    @Test
    fun `updateTags invoked on reader tab content is first displayed`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(-1)
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `updateTags NOT invoked if lastUpdate within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD + 1)
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNull()
    }

    @Test
    fun `updateTags invoked if lastUpdate NOT within threshold`() = testWithEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.readerTagsUpdatedTimestamp).thenReturn(DUMMY_CURRENT_TIME - UPDATE_TAGS_THRESHOLD - 1)
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(viewModel.updateTags.value?.getContentIfNotHandled()).isNotNull
    }

    @Test
    fun `UiState is NOT updated with content state when loaded tags are empty`() = test {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(ReaderTagList())
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(state).isNotInstanceOf(ContentUiState::class.java)
    }

    @Test
    fun `UiState is updated with content state when loaded tags are NOT empty`() = testWithNonEmptyTags {
        // Arrange
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(state).isInstanceOf(ContentUiState::class.java)
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
        assertThat(state).isInstanceOf(ContentUiState::class.java)
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
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(tabPosition).isEqualTo(3)
    }

    @Test
    fun `SelectTab is invoked when last selected tab is null`() = testWithNonMockedNonEmptyTags {
        // Arrange
        whenever(appPrefsWrapper.getReaderTag()).thenReturn(null)

        var tabPosition: TabPosition? = null
        viewModel.selectTab.observeForever {
            tabPosition = it.getContentIfNotHandled()
        }
        // Act
        triggerReaderTabContentDisplay()
        // Assert
        assertThat(tabPosition).isGreaterThan(-1)
    }

    @Test
    fun `SelectTab when tags are empty`() = testWithEmptyTags {
        // Arrange
        var tabPosition: TabPosition? = null
        viewModel.selectTab.observeForever {
            tabPosition = it.getContentIfNotHandled()
        }
        // Act
        triggerReaderTabContentDisplay()
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
        triggerReaderTabContentDisplay()
        viewModel.selectedTabChange(readerTag)

        // Assert
        assertThat(tabPosition).isEqualTo(2)
    }

    @Test
    fun `OnSearchActionClicked emits showSearch event`() {
        // Arrange
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var event: Event<Unit>? = null
        viewModel.showSearch.observeForever {
            event = it
        }
        // Act
        viewModel.onSearchActionClicked()

        // Assert
        assertThat(event).isNotNull
    }

    @Test
    fun `Search is disabled for self-hosted login`() = testWithNonEmptyTags {
        // Arrange
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        triggerReaderTabContentDisplay()

        // Assert
        assertThat(state!!.searchIconVisible).isFalse()
    }

    @Test
    fun `Search is enabled for dot com login`() = testWithNonEmptyTags {
        // Arrange
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        var state: ReaderUiState? = null
        viewModel.uiState.observeForever {
            state = it
        }
        // Act
        triggerReaderTabContentDisplay()

        // Assert
        assertThat(state!!.searchIconVisible).isTrue()
    }

    private fun triggerReaderTabContentDisplay() {
        if (appPrefsWrapper.isReaderImprovementsPhase2Enabled()) {
            viewModel.start()
            viewModel.onCloseReaderInterests()
        } else {
            viewModel.start()
        }
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

    private fun <T> testWithNonMockedNonEmptyTags(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(loadReaderTabsUseCase.loadTabs()).thenReturn(createNonMockedNonEmptyReaderTagList())
            block()
        }
    }

    private fun createNonMockedNonEmptyReaderTagList(): ReaderTagList {
        return ReaderTagList().apply {
            add(ReaderTag("Following", "Following", "Following", FOLLOWING_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Discover", "Discover", "Discover", DISCOVER_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Like", "Like", "Like", LIKED_PATH, ReaderTagType.DEFAULT))
            add(ReaderTag("Saved", "Saved", "Saved", "Saved", ReaderTagType.DEFAULT))
        }
    }
}
