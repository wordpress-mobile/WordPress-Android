package org.wordpress.android.ui.main.jetpack

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
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
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    var isInitialized = false

    fun start() {
        if (isInitialized) return
        isInitialized = true
        initState()
    }

    private fun initState() {
        _uiState.value = UiState(
                userAvatarUrl = getAvatarUrl(),
                stepState = StepUiState.Welcome(sites = getSiteList()),
        )
    }

    private fun getSiteList(): List<SiteListItemUiState> {
        return siteStore.sites.map { site ->
            SiteListItemUiState(
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

    data class UiState(
        val userAvatarUrl: String = "",
        val stepState: StepUiState? = null,
    )

    sealed class StepUiState(
        val screenIconRes: Int,
        val title: UiString,
        val subtitle: UiString,
        val message: UiString,
    ) {
        data class Welcome(
            val sites: List<SiteListItemUiState>
        ) : StepUiState(
                screenIconRes = R.drawable.ic_wordpress_jetpack_logo,
                title = UiStringRes(R.string.jp_welcome_title),
                subtitle = UiStringRes(R.string.jp_welcome_subtitle),
                message = UiStringRes(
                        if (sites.size > 1) {
                            R.string.jp_welcome_sites_found_message
                        } else {
                            R.string.jp_welcome_site_found_message
                        }
                )
        )
    }

    data class SiteListItemUiState(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )
}
