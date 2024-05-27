package org.wordpress.android.ui.debug.preferences

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject

@HiltViewModel
class DebugSharedPreferenceFlagsViewModel @Inject constructor(
    private val prefsWrapper: AppPrefsWrapper
) : ViewModel() {
    private val _uiStateFlow = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val uiStateFlow = _uiStateFlow.asStateFlow()

    init {
        val flags = prefsWrapper.getAllPrefs().mapNotNull { (key, value) ->
            if (value is Boolean) key to value else null
        }.toMap()

        val explicitFlags = DebugPrefs.entries.mapNotNull {
            // Only supporting boolean for now.
            if (it.type == Boolean::class) it else null
        }.associate { it.key to prefsWrapper.getDebugBooleanPref(it.key, false) }
        
        _uiStateFlow.value = flags + explicitFlags
    }

    fun setFlag(key: String, value: Boolean) {
        prefsWrapper.putBoolean({ key }, value)
        _uiStateFlow.value = _uiStateFlow.value.toMutableMap().apply { this[key] = value }
    }
}
