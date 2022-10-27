package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Content
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Initial
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.SiteListItem
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

const val SITE_ICON_SIZE = 60 * 3

@HiltViewModel
class JetpackWelcomeViewModel @Inject constructor(
    val siteStore: SiteStore,
    val siteUtilsWrapper: SiteUtilsWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<JetpackWelcomeUiState>(Initial)
    val uiState: StateFlow<JetpackWelcomeUiState> = _uiState

    fun start() {
        initOrRestoreUiState()
    }

    private fun initOrRestoreUiState() {
        getSiteList()
    }

    private fun getSiteList() {
        val sites = siteStore.sites
        val siteListItems = sites.map { site ->
            SiteListItem(
                    id = site.siteId,
                    name = siteUtilsWrapper.getSiteNameOrHomeURL(site),
                    url = siteUtilsWrapper.getHomeURLOrHostName(site),
                    iconUrl = siteUtilsWrapper.getSiteIconUrl(site, SITE_ICON_SIZE),
            )
        }

        postUiState(Content.SiteList(siteListItems))
    }

    private fun postUiState(state: JetpackWelcomeUiState) {
        viewModelScope.launch {
            _uiState.value = state
        }
    }
}

sealed class JetpackWelcomeUiState {
    object Initial : JetpackWelcomeUiState()

    sealed class Content : JetpackWelcomeUiState() {
        abstract val title: UiString
        abstract val subtitle: UiString
        abstract val message: UiString

        data class SiteList(
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
