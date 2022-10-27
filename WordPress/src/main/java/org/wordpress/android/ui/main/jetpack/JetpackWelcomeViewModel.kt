package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Content
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.Initial
import org.wordpress.android.ui.main.jetpack.JetpackWelcomeUiState.SiteListItem
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

const val SITE_ICON_SIZE = 60 * 3
const val USER_AVATAR_SIZE = 32 * 3

@HiltViewModel
class JetpackWelcomeViewModel @Inject constructor(
    val siteStore: SiteStore,
    val accountStore: AccountStore,
    val siteUtilsWrapper: SiteUtilsWrapper,
    val gravatarUtilsWrapper: GravatarUtilsWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<JetpackWelcomeUiState>(Initial)
    val uiState: StateFlow<JetpackWelcomeUiState> = _uiState

    fun start() {
        if (_uiState.value !is Initial) return
        initState()
    }

    private fun initState() {
        val siteList = getSiteList()
        val avatarUrl = getAvatarUrl()

        _uiState.value = Content.SiteList(
                avatarUrl = avatarUrl,
                sites = siteList,
        )
    }

    private fun getSiteList(): List<SiteListItem> {
        return siteStore.sites.map { site ->
            SiteListItem(
                    id = site.siteId,
                    name = siteUtilsWrapper.getSiteNameOrHomeURL(site),
                    url = siteUtilsWrapper.getHomeURLOrHostName(site),
                    iconUrl = siteUtilsWrapper.getSiteIconUrl(site, SITE_ICON_SIZE),
            )
        }
    }

    private fun getAvatarUrl(): String {
        return gravatarUtilsWrapper.fixGravatarUrl(accountStore.account?.avatarUrl.orEmpty(), USER_AVATAR_SIZE)
    }
}

sealed class JetpackWelcomeUiState {
    object Initial : JetpackWelcomeUiState()

    sealed class Content : JetpackWelcomeUiState() {
        abstract val avatarUrl: String
        abstract val screenIconRes: Int
        abstract val title: UiString
        abstract val subtitle: UiString
        abstract val message: UiString

        data class SiteList(
            override val avatarUrl: String,
            val sites: List<SiteListItem>
        ) : Content() {
            override val screenIconRes = R.drawable.ic_wordpress_jetpack_logo
            override val title = UiStringRes(R.string.jp_welcome_title)
            override val subtitle = UiStringRes(R.string.jp_welcome_subtitle)
            override val message = if (sites.size > 1) {
                UiStringRes(R.string.jp_welcome_sites_found_message)
            } else {
                UiStringRes(R.string.jp_welcome_site_found_message)
            }
        }
    }

    data class SiteListItem(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )
}
