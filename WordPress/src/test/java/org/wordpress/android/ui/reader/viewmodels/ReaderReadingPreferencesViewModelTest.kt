package org.wordpress.android.ui.reader.viewmodels

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSaveReadingPreferencesUseCase
import org.wordpress.android.ui.reader.viewmodels.ReaderReadingPreferencesViewModel.ActionEvent
import org.wordpress.android.util.config.ReaderReadingPreferencesFeedbackFeatureConfig

@ExperimentalCoroutinesApi
class ReaderReadingPreferencesViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase

    @Mock
    lateinit var saveReadingPreferences: ReaderSaveReadingPreferencesUseCase

    @Mock
    lateinit var readingPreferencesFeedbackFeatureConfig: ReaderReadingPreferencesFeedbackFeatureConfig

    @Mock
    lateinit var readingPreferencesTracker: ReaderReadingPreferencesTracker

    private val viewModelDispatcher = UnconfinedTestDispatcher(testDispatcher().scheduler)
    private lateinit var viewModel: ReaderReadingPreferencesViewModel

    private val collectedEvents = mutableListOf<ActionEvent>()

    @Before
    fun setUp() {
        whenever(getReadingPreferences()).thenReturn(DEFAULT_READING_PREFERENCES)

        viewModel = ReaderReadingPreferencesViewModel(
            getReadingPreferences,
            saveReadingPreferences,
            readingPreferencesFeedbackFeatureConfig,
            readingPreferencesTracker,
            viewModelDispatcher,
        )

        viewModel.collectEvents()
    }

    @After
    fun tearDown() {
        viewModelDispatcher.cancel()
        collectedEvents.clear()
    }

    private fun ReaderReadingPreferencesViewModel.collectEvents() {
        actionEvents.onEach { actionEvent ->
            collectedEvents.add(actionEvent)
        }.launchIn(testScope().backgroundScope)
    }

    @Test
    fun `when ViewModel is initialized then it should emit UpdateStatusBarColor action event`() = test {
        // When
        viewModel.init()

        // Then
        val updateStatusBarColorEvent = collectedEvents.last() as ActionEvent.UpdateStatusBarColor
        assertThat(updateStatusBarColorEvent.theme).isEqualTo(DEFAULT_READING_PREFERENCES.theme)
    }

    @Test
    fun `when collecting currentReadingPreferences then it should have the initial reading preferences`() = test {
        // When
        val currentReadingPreferences = viewModel.currentReadingPreferences.first()

        // Then
        assertThat(currentReadingPreferences).isEqualTo(DEFAULT_READING_PREFERENCES)
    }

    @Test
    fun `when onThemeClick is called then it should update the theme`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.OLED

        // When
        viewModel.onThemeClick(newTheme)

        // Then
        val updatedReadingPreferences = viewModel.currentReadingPreferences.first()
        assertThat(updatedReadingPreferences.theme).isEqualTo(newTheme)
    }

    @Test
    fun `when onFontFamilyClick is called then it should update the font family`() = test {
        // Given
        val newFontFamily = ReaderReadingPreferences.FontFamily.MONO

        // When
        viewModel.onFontFamilyClick(newFontFamily)

        // Then
        val updatedReadingPreferences = viewModel.currentReadingPreferences.first()
        assertThat(updatedReadingPreferences.fontFamily).isEqualTo(newFontFamily)
    }

    @Test
    fun `when onFontSizeClick is called then it should update the font size`() = test {
        // Given
        val newFontSize = ReaderReadingPreferences.FontSize.LARGE

        // When
        viewModel.onFontSizeClick(newFontSize)

        // Then
        val updatedReadingPreferences = viewModel.currentReadingPreferences.first()
        assertThat(updatedReadingPreferences.fontSize).isEqualTo(newFontSize)
    }

    @Test
    fun `when onExitActionClick is called then it emits Close action event`() = test {
        // When
        viewModel.onExitActionClick()

        // Then
        val closeEvent = collectedEvents.last()
        assertThat(closeEvent).isEqualTo(ActionEvent.Close)
    }

    @Test
    fun `when onExitActionClick is called with original preferences then it doesn't save them`() =
        test {
            // When
            viewModel.onExitActionClick()

            // Then
            verifyNoInteractions(saveReadingPreferences)
        }

    @Test
    fun `when onExitActionClick is called with updated preferences then it saves them`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.SOFT
        viewModel.onThemeClick(newTheme)

        // When
        viewModel.onExitActionClick()

        // Then
        verify(saveReadingPreferences).invoke(argThat { theme == newTheme })
    }

    @Test
    fun `when onBottomSheetHidden is called with original preferences then it doesn't save them`() =
        test {
            // When
            viewModel.onBottomSheetHidden()

            // Then
            verifyNoInteractions(saveReadingPreferences)
        }

    @Test
    fun `when onBottomSheetHidden is called with updated preferences then it saves them`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.SOFT
        viewModel.onThemeClick(newTheme)

        // When
        viewModel.onBottomSheetHidden()

        // Then
        verify(saveReadingPreferences).invoke(argThat { theme == newTheme })
    }

    @Test
    fun `when onScreenClosed is called with original preferences then it doesn't emit UpdatePostDetail`() = test {
        // Given
        viewModel.onExitActionClick()

        // When
        viewModel.onScreenClosed()

        // Then
        val updateEvent = collectedEvents.last()
        assertThat(updateEvent).isNotEqualTo(ActionEvent.UpdatePostDetails)
    }

    @Test
    fun `when onScreenClosed is called with updated preferences then it emits UpdatePostDetail`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.SOFT
        viewModel.onThemeClick(newTheme)
        viewModel.onExitActionClick()

        // When
        viewModel.onScreenClosed()

        // Then
        val updateEvent = collectedEvents.last()
        assertThat(updateEvent).isEqualTo(ActionEvent.UpdatePostDetails)
    }

    @Test
    fun `when onSendFeedbackClick is called then it emits OpenWebView action event`() = test {
        // When
        viewModel.onSendFeedbackClick()

        // Then
        val openWebViewEvent = collectedEvents.last() as ActionEvent.OpenWebView
        assertThat(openWebViewEvent.url).isEqualTo(EXPECTED_FEEDBACK_URL)
    }

    @Test
    fun `when readerReadingPreferencesFeedbackFeatureConfig is true then isFeedbackEnabled emits true`() = test {
        // Given
        whenever(readingPreferencesFeedbackFeatureConfig.isEnabled()).thenReturn(true)

        // When
        viewModel.init()

        // Then
        val isFeedbackEnabled = viewModel.isFeedbackEnabled.first()
        assertThat(isFeedbackEnabled).isTrue()
    }

    @Test
    fun `when readerReadingPreferencesFeedbackFeatureConfig is false then isFeedbackEnabled emits false`() = test {
        // Given
        whenever(readingPreferencesFeedbackFeatureConfig.isEnabled()).thenReturn(false)

        // When
        viewModel.init()

        // Then
        val isFeedbackEnabled = viewModel.isFeedbackEnabled.first()
        assertThat(isFeedbackEnabled).isFalse()
    }

    // analytics tests
    @Test
    fun `when onScreenOpened is called then it should track the screen opened event`() = test {
        ReaderReadingPreferencesTracker.Source.values().forEach { source ->
            // When
            viewModel.onScreenOpened(source)

            // Then
            verify(readingPreferencesTracker).trackScreenOpened(source)
        }
    }

    @Test
    fun `when onScreenClosed is called then it should track the screen closed event`() = test {
        // When
        viewModel.onScreenClosed()

        // Then
        verify(readingPreferencesTracker).trackScreenClosed()
    }

    @Test
    fun `when onSendFeedbackClick is called then it should track the feedback tapped event`() = test {
        // When
        viewModel.onSendFeedbackClick()

        // Then
        verify(readingPreferencesTracker).trackFeedbackTapped()
    }

    @Test
    fun `when onThemeClick is called then it should track the theme tapped event`() = test {
        ReaderReadingPreferences.Theme.values().forEach { theme ->
            // When
            viewModel.onThemeClick(theme)

            // Then
            verify(readingPreferencesTracker).trackItemTapped(theme)
        }
    }

    @Test
    fun `when onFontFamilyClick is called then it should track the font family tapped event`() = test {
        ReaderReadingPreferences.FontFamily.values().forEach { fontFamily ->
            // When
            viewModel.onFontFamilyClick(fontFamily)

            // Then
            verify(readingPreferencesTracker).trackItemTapped(fontFamily)
        }
    }

    @Test
    fun `when onFontSizeClick is called then it should track the font size tapped event`() = test {
        ReaderReadingPreferences.FontSize.values().forEach { fontSize ->
            // When
            viewModel.onFontSizeClick(fontSize)

            // Then
            verify(readingPreferencesTracker).trackItemTapped(fontSize)
        }
    }

    @Test
    fun `when saveReadingPreferencesAndClose is called then it should track the saved event`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.SOFT
        viewModel.onThemeClick(newTheme)

        // When
        viewModel.onExitActionClick()

        // Then
        verify(readingPreferencesTracker).trackSaved(argThat { theme == newTheme })
    }

    companion object {
        private val DEFAULT_READING_PREFERENCES = ReaderReadingPreferences()
        private const val EXPECTED_FEEDBACK_URL = "https://automattic.survey.fm/reader-customization-survey"
    }
}
