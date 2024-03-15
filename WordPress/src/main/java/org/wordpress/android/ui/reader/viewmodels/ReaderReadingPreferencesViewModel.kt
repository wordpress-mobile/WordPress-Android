package org.wordpress.android.ui.reader.viewmodels

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.BufferOverflow
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
    private val getReadingPreferences: ReaderGetReadingPreferencesSyncUseCase,
    private val saveReadingPreferences: ReaderSaveReadingPreferencesUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _currentReadingPreferences = MutableStateFlow(getReadingPreferences())
    val currentReadingPreferences: StateFlow<ReaderReadingPreferences> = _currentReadingPreferences

    private val _actionEvents = MutableSharedFlow<ActionEvent>(onBufferOverflow = BufferOverflow.SUSPEND)
    val actionEvents: SharedFlow<ActionEvent> = _actionEvents

    fun init() {
        launch {
            val readingPreferences = getReadingPreferences()
            _currentReadingPreferences.update { readingPreferences }
            _actionEvents.emit(ActionEvent.UpdateStatusBarColor(readingPreferences.theme))
        }
    }

    fun onThemeClick(theme: ReaderReadingPreferences.Theme) {
        val previousBackgroundColor = currentReadingPreferences.value.theme.backgroundColorRes
        _currentReadingPreferences.update { it.copy(theme = theme) }
        launch {
            if (previousBackgroundColor != theme.backgroundColorRes) {
                _actionEvents.emit(ActionEvent.UpdateStatusBarColor(theme))
            }
        }
    }

    fun onFontFamilyClick(fontFamily: ReaderReadingPreferences.FontFamily) {
        _currentReadingPreferences.update { it.copy(fontFamily = fontFamily) }
    }

    fun onFontSizeClick(fontSize: ReaderReadingPreferences.FontSize) {
        _currentReadingPreferences.update { it.copy(fontSize = fontSize) }
    }

    fun saveReadingPreferencesAndClose() {
        launch {
            saveReadingPreferences(currentReadingPreferences.value)
            _actionEvents.emit(ActionEvent.Close)
        }
    }

    sealed interface ActionEvent {
        data object Close : ActionEvent
        data class UpdateStatusBarColor(val theme: ReaderReadingPreferences.Theme) : ActionEvent
    }
}
