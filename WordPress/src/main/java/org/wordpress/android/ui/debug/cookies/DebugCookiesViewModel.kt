package org.wordpress.android.ui.debug.cookies

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.wordpress.android.ui.debug.cookies.DebugCookiesAdapter.DebugCookieItem
import org.wordpress.android.ui.utils.ListItemInteraction
import javax.inject.Inject

class DebugCookiesViewModel @Inject constructor(
    private val debugCookieManager: DebugCookieManager
) : ViewModel() {
    private val _uiState = MutableLiveData(UiState(getUpdatedItems()))
    val uiState: LiveData<UiState> = _uiState

    fun setCookie(domain: String?, name: String?, value: String?) {
        if (!domain.isNullOrBlank() && !name.isNullOrBlank()) {
            debugCookieManager.add(DebugCookie(domain, name, value))
            _uiState.value = UiState(
                    items = getUpdatedItems(),
                    domainInputText = domain,
                    nameInputText = name,
                    valueInputText = value
            )
        }
    }

    private fun onItemClick(debugCookie: DebugCookie) {
        _uiState.value = _uiState.value?.copy(
                domainInputText = debugCookie.domain,
                nameInputText = debugCookie.name,
                valueInputText = debugCookie.value
        )
    }

    private fun onDeleteClick(debugCookie: DebugCookie) {
        debugCookieManager.remove(debugCookie)
        _uiState.value = _uiState.value?.copy(
                items = getUpdatedItems()
        )
    }

    private fun getUpdatedItems() = debugCookieManager.getAll().sortedBy { it.key }.map {
        DebugCookieItem(
                it.key,
                it.domain,
                it.name,
                it.value,
                ListItemInteraction.create(it, ::onItemClick),
                ListItemInteraction.create(it, ::onDeleteClick)
        )
    }

    data class UiState(
        val items: List<DebugCookieItem>,
        val domainInputText: String? = null,
        val nameInputText: String? = null,
        val valueInputText: String? = null
    )
}
