package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

@HiltViewModel
class JetpackWelcomeViewModel @Inject constructor(
) : ViewModel() {
    private val _uiState = MutableStateFlow<JetpackWelcomeUiState>(
            JetpackWelcomeUiState.Content.SiteList(previewSiteListItems())
    )
    val uiState: StateFlow<JetpackWelcomeUiState> = _uiState

    fun start() {
        initOrRestoreUiState()
    }

    private fun initOrRestoreUiState() {
        // TODO load sites list
    }
}

sealed class JetpackWelcomeUiState {
    object Initial : JetpackWelcomeUiState()

    sealed class Content: JetpackWelcomeUiState() {
        abstract val title: UiString
        abstract val subtitle: UiString
        abstract val message: UiString

        data class SiteList (
            val sites: List<SiteListItem>
        ) : Content() {
            override val title = UiStringRes(R.string.jp_welcome_title)
            override val subtitle = UiStringRes(R.string.jp_welcome_subtitle)
            override val message = UiStringRes(R.string.jp_welcome_sites_found_message)
        }
    }

    object Error : JetpackWelcomeUiState()

    data class SiteListItem(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )
}
