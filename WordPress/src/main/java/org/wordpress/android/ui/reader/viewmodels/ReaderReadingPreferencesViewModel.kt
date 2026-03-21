package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.reader.models.ReaderReadingPreferences
import org.wordpress.android.ui.reader.tracker.ReaderReadingPreferencesTracker
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSaveReadingPreferencesUseCase
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderReadingPreferencesViewModel @Inject constructor(
    getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase,
    private val saveReadingPreferences: ReaderSaveReadingPreferencesUseCase,
    private val readingPreferencesTracker: ReaderReadingPreferencesTracker,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val originalReadingPreferences = getReadingPreferences()
    private val _currentReadingPreferences = MutableStateFlow(originalReadingPreferences)
    val currentReadingPreferences: StateFlow<ReaderReadingPreferences> = _currentReadingPreferences

    val hasUnsavedChanges: StateFlow<Boolean> = _currentReadingPreferences.map {
        it != originalReadingPreferences
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: SharedFlow<ActionEvent> = _actionEvents

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

    /**
     * Save the current preferences and dismiss the dialog.
     */
    fun onSaveClick() {
        launch {
            val currentPreferences = currentReadingPreferences.value
            if (currentPreferences != originalReadingPreferences) {
                saveReadingPreferences(currentPreferences)
                readingPreferencesTracker.trackSaved(currentPreferences)
            }
            _actionEvents.emit(ActionEvent.SaveAndClose)
        }
    }

    /**
     * Discard changes and dismiss the dialog.
     */
    fun onCloseClick() {
        launch {
            _actionEvents.emit(ActionEvent.Close)
        }
    }

    sealed interface ActionEvent {
        data object Close : ActionEvent
        data object SaveAndClose : ActionEvent
    }
}
