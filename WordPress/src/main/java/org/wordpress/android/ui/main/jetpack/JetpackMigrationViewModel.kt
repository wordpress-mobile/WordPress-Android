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
class JetpackMigrationViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onAccountInfoLoaded() {
        _uiState.value = _uiState.value.copy(userAvatarUrl = getAvatarUrl())
    }

    fun onSiteListLoaded() {
        _uiState.value = _uiState.value.copy(
                stepState = StepUiState.Welcome(
                        sites = getSiteList(),
                        primaryActionButton = ActionButton.WelcomePrimaryButton(::onContinueClicked),
                        secondaryActionButton = ActionButton.WelcomeSecondaryButton(::onHelpClicked),
                ),
        )
    }
    private fun onContinueClicked() {
        // TODO Handle on Continue
    }

    private fun onHelpClicked() {
        // TODO Handle on Help
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
        open val primaryActionButton: ActionButton,
        open val secondaryActionButton: ActionButton,
    ) {
        data class Welcome(
            val sites: List<SiteListItemUiState>,
            override val primaryActionButton: ActionButton,
            override val secondaryActionButton: ActionButton,
        ) : StepUiState(
                primaryActionButton = primaryActionButton,
                secondaryActionButton = secondaryActionButton,
                screenIconRes = R.drawable.ic_wordpress_jetpack_logo,
                title = UiStringRes(R.string.jp_migration_welcome_title),
                subtitle = UiStringRes(R.string.jp_migration_welcome_subtitle),
                message = UiStringRes(
                        if (sites.size > 1) {
                            R.string.jp_migration_welcome_sites_found_message
                        } else {
                            R.string.jp_migration_welcome_site_found_message
                        }
                ),
        )
    }

    data class SiteListItemUiState(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )

    sealed class ActionButton(
        val text: UiString,
        open val onClick: () -> Unit,
    ) {
        data class WelcomePrimaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_continue_button),
        )

        data class WelcomeSecondaryButton(
            override val onClick: () -> Unit,
        ) : ActionButton(
                onClick = onClick,
                text = UiStringRes(R.string.jp_migration_help_button),
        )
    }
}
