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
import org.wordpress.android.ui.reader.usecases.ReaderGetReadingPreferencesSyncUseCase
import org.wordpress.android.ui.reader.usecases.ReaderSaveReadingPreferencesUseCase
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class ReaderReadingPreferencesViewModel @Inject constructor(
    getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase,
    private val saveReadingPreferences: ReaderSaveReadingPreferencesUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val originalReadingPreferences = getReadingPreferences()
    private val _currentReadingPreferences = MutableStateFlow(originalReadingPreferences)
    val currentReadingPreferences: StateFlow<ReaderReadingPreferences> = _currentReadingPreferences

    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: SharedFlow<ActionEvent> = _actionEvents

    fun init() {
        launch {
            _actionEvents.emit(ActionEvent.UpdateStatusBarColor(originalReadingPreferences.theme))
        }
    }

    fun onThemeClick(theme: ReaderReadingPreferences.Theme) {
        _currentReadingPreferences.update { it.copy(theme = theme) }
    }

    fun onFontFamilyClick(fontFamily: ReaderReadingPreferences.FontFamily) {
        _currentReadingPreferences.update { it.copy(fontFamily = fontFamily) }
    }

    fun onFontSizeClick(fontSize: ReaderReadingPreferences.FontSize) {
        _currentReadingPreferences.update { it.copy(fontSize = fontSize) }
    }

    fun saveReadingPreferencesAndClose() {
        launch {
            if (currentReadingPreferences.value != originalReadingPreferences) {
                saveReadingPreferences(currentReadingPreferences.value)
                val isDirty = currentReadingPreferences.value != originalReadingPreferences
                _actionEvents.emit(ActionEvent.Close(isDirty = isDirty))
            } else {
                _actionEvents.emit(ActionEvent.Close(isDirty = false))
            }
        }
    }

    sealed interface ActionEvent {
        data class Close(val isDirty: Boolean) : ActionEvent
        data class UpdateStatusBarColor(val theme: ReaderReadingPreferences.Theme) : ActionEvent
    }
}
