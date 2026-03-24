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

@ExperimentalCoroutinesApi
class ReaderReadingPreferencesViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase

    @Mock
    lateinit var saveReadingPreferences: ReaderSaveReadingPreferencesUseCase

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
    fun `when collecting currentReadingPreferences then it should have the initial reading preferences`() =
        test {
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
    fun `when onSaveClick is called then it emits SaveAndClose action event`() = test {
        // When
        viewModel.onSaveClick()

        // Then
        val event = collectedEvents.last()
        assertThat(event).isEqualTo(ActionEvent.SaveAndClose)
    }

    @Test
    fun `when onSaveClick is called with original preferences then it doesn't save them`() =
        test {
            // When
            viewModel.onSaveClick()

            // Then
            verifyNoInteractions(saveReadingPreferences)
        }

    @Test
    fun `when onSaveClick is called with updated preferences then it saves them`() = test {
        // Given
        val newTheme = ReaderReadingPreferences.Theme.SOFT
        viewModel.onThemeClick(newTheme)

        // When
        viewModel.onSaveClick()

        // Then
        verify(saveReadingPreferences).invoke(argThat { theme == newTheme })
    }

    @Test
    fun `when onCloseClick is called then it emits Close action event`() = test {
        // When
        viewModel.onCloseClick()

        // Then
        val event = collectedEvents.last()
        assertThat(event).isEqualTo(ActionEvent.Close)
    }

    @Test
    fun `when onCloseClick is called then it does not save preferences`() = test {
        // Given
        viewModel.onThemeClick(ReaderReadingPreferences.Theme.SOFT)

        // When
        viewModel.onCloseClick()

        // Then
        verifyNoInteractions(saveReadingPreferences)
    }

    @Test
    fun `hasUnsavedChanges is false initially`() = test {
        // When
        val result = viewModel.hasUnsavedChanges.first()

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `hasUnsavedChanges is true after changing theme`() = test {
        // When
        viewModel.onThemeClick(ReaderReadingPreferences.Theme.OLED)

        // Then
        val result = viewModel.hasUnsavedChanges.first()
        assertThat(result).isTrue()
    }

    @Test
    fun `hasUnsavedChanges is false after reverting to original`() = test {
        // Given
        viewModel.onThemeClick(ReaderReadingPreferences.Theme.OLED)

        // When
        viewModel.onThemeClick(DEFAULT_READING_PREFERENCES.theme)

        // Then
        val result = viewModel.hasUnsavedChanges.first()
        assertThat(result).isFalse()
    }

    // analytics tests
    @Test
    fun `when onScreenOpened is called then it should track the screen opened event`() = test {
        ReaderReadingPreferencesTracker.Source.entries.forEach { source ->
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
    fun `when onThemeClick is called then it should track the theme tapped event`() = test {
        ReaderReadingPreferences.Theme.entries.forEach { theme ->
            // When
            viewModel.onThemeClick(theme)

            // Then
            verify(readingPreferencesTracker).trackItemTapped(theme)
        }
    }

    @Test
    fun `when onFontFamilyClick is called then it should track the font family tapped event`() =
        test {
            ReaderReadingPreferences.FontFamily.entries.forEach { fontFamily ->
                // When
                viewModel.onFontFamilyClick(fontFamily)

                // Then
                verify(readingPreferencesTracker).trackItemTapped(fontFamily)
            }
        }

    @Test
    fun `when onFontSizeClick is called then it should track the font size tapped event`() =
        test {
            ReaderReadingPreferences.FontSize.entries.forEach { fontSize ->
                // When
                viewModel.onFontSizeClick(fontSize)

                // Then
                verify(readingPreferencesTracker).trackItemTapped(fontSize)
            }
        }

    @Test
    fun `when onSaveClick is called with changes then it should track the saved event`() =
        test {
            // Given
            val newTheme = ReaderReadingPreferences.Theme.SOFT
            viewModel.onThemeClick(newTheme)

            // When
            viewModel.onSaveClick()

            // Then
            verify(readingPreferencesTracker).trackSaved(
                argThat { theme == newTheme }
            )
        }

    companion object {
        private val DEFAULT_READING_PREFERENCES = ReaderReadingPreferences()
    }
}
