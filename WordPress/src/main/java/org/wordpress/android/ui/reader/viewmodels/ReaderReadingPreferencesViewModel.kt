package org.wordpress.android.ui.reader.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSaveReadingPreferencesUseCase
import org.wordpress.android.util.config.ReaderReadingPreferencesFeedbackFeatureConfig
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderReadingPreferencesViewModel @Inject constructor(
    getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase,
    private val saveReadingPreferences: ReaderSaveReadingPreferencesUseCase,
    private val readingPreferencesFeedbackFeatureConfig: ReaderReadingPreferencesFeedbackFeatureConfig,
    private val readingPreferencesTracker: ReaderReadingPreferencesTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val originalReadingPreferences = getReadingPreferences()
    private val _currentReadingPreferences = MutableStateFlow(originalReadingPreferences)
    val currentReadingPreferences: StateFlow<ReaderReadingPreferences> = _currentReadingPreferences

    private val _isFeedbackEnabled = MutableStateFlow(false)
    val isFeedbackEnabled: StateFlow<Boolean> = _isFeedbackEnabled

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: SharedFlow<ActionEvent> = _actionEvents

    fun init() {
        launch {
            _isFeedbackEnabled.emit(readingPreferencesFeedbackFeatureConfig.isEnabled())
            _actionEvents.emit(ActionEvent.UpdateStatusBarColor(originalReadingPreferences.theme))
        }
    }

    fun onScreenOpened(source: ReaderReadingPreferencesTracker.Source) {
        readingPreferencesTracker.trackScreenOpened(source)
    }

    fun onScreenClosed() {
        readingPreferencesTracker.trackScreenClosed()
    }

    fun onThemeClick(theme: ReaderReadingPreferences.Theme) {
        _currentReadingPreferences.update { it.copy(theme = theme) }
        readingPreferencesTracker.trackItemTapped(theme)
    }

    fun onFontFamilyClick(fontFamily: ReaderReadingPreferences.FontFamily) {
        _currentReadingPreferences.update { it.copy(fontFamily = fontFamily) }
        readingPreferencesTracker.trackItemTapped(fontFamily)
    }

    fun onFontSizeClick(fontSize: ReaderReadingPreferences.FontSize) {
        _currentReadingPreferences.update { it.copy(fontSize = fontSize) }
        readingPreferencesTracker.trackItemTapped(fontSize)
    }

    fun saveReadingPreferencesAndClose() {
        launch {
            val currentPreferences = currentReadingPreferences.value
            val isDirty = currentPreferences != originalReadingPreferences
            if (isDirty) {
                saveReadingPreferences(currentPreferences)
                readingPreferencesTracker.trackSaved(currentPreferences)
            }
            _actionEvents.emit(ActionEvent.Close(isDirty))
        }
    }

    fun onSendFeedbackClick() {
        launch {
            readingPreferencesTracker.trackFeedbackTapped()
            _actionEvents.emit(ActionEvent.OpenWebView(FEEDBACK_URL))
        }
    }

    sealed interface ActionEvent {
        data class Close(val isDirty: Boolean) : ActionEvent
        data class UpdateStatusBarColor(val theme: ReaderReadingPreferences.Theme) : ActionEvent
        data class OpenWebView(val url: String) : ActionEvent
    }

    companion object {
        private const val FEEDBACK_URL = "https://automattic.survey.fm/reader-customization-survey"
    }
}
