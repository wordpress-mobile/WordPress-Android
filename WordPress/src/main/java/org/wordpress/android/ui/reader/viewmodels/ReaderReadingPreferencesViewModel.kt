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
        if (isDirty()) {
            // here we assume the code for saving preferences has been called before reaching this point
            launch {
                _actionEvents.emit(ActionEvent.UpdatePostDetails)
            }
        }
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

    /**
     * An exit action has been triggered by the user. This means that we need to save the current preferences and emit
     * the close event, so the dialog is dismissed.
     */
    fun onExitActionClick() {
        launch {
            saveReadingPreferencesInternal()
            _actionEvents.emit(ActionEvent.Close)
        }
    }

    /**
     * The bottom sheet has been hidden by the user, which means the dismiss process is already on its way. All we need
     * to do is save the current preferences.
     */
    fun onBottomSheetHidden() {
        launch {
            saveReadingPreferencesInternal()
        }
    }

    fun onSendFeedbackClick() {
        launch {
            readingPreferencesTracker.trackFeedbackTapped()
            _actionEvents.emit(ActionEvent.OpenWebView(FEEDBACK_URL))
        }
    }

    private suspend fun saveReadingPreferencesInternal() {
        val currentPreferences = currentReadingPreferences.value
        if (isDirty()) {
            saveReadingPreferences(currentPreferences)
            readingPreferencesTracker.trackSaved(currentPreferences)
        }
    }

    private fun isDirty(): Boolean = currentReadingPreferences.value != originalReadingPreferences

    sealed interface ActionEvent {
        data object Close : ActionEvent
        data object UpdatePostDetails : ActionEvent
        data class UpdateStatusBarColor(val theme: ReaderReadingPreferences.Theme) : ActionEvent
        data class OpenWebView(val url: String) : ActionEvent
    }

    companion object {
        private const val FEEDBACK_URL = "https://automattic.survey.fm/reader-customization-survey"
    }
}
