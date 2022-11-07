package org.wordpress.android.ui.main.jetpack.migration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.wordpress.android.R
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Content
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.UiState.Loading
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.GravatarUtilsWrapper
import org.wordpress.android.util.SiteUtilsWrapper
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val accountStore: AccountStore,
    private val siteUtilsWrapper: SiteUtilsWrapper,
    private val gravatarUtilsWrapper: GravatarUtilsWrapper,
    private val appPrefsWrapper: AppPrefsWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState>(Loading)
    val uiState: StateFlow<UiState> = _uiState

    // TODO Review this after data sync work is done
    fun onAccountInfoLoaded() {
        if (isDataAvailable()) {
            initWelcomeState()
        }
    }

    // TODO Review this after data sync work is done
    fun onSiteListLoaded() {
        if (isDataAvailable()) {
            initWelcomeState()
        }
    }

    private fun isDataAvailable() = accountStore.account.userName.isNotEmpty() && siteStore.sites.isNotEmpty()

    private fun initWelcomeState() {
        _uiState.value = Content.Welcome(
                userAvatarUrl = getAvatarUrl(),
                sites = getSiteList(),
                primaryActionButton = WelcomePrimaryButton(::onContinueClicked),
                secondaryActionButton = WelcomeSecondaryButton(::onHelpClicked),
        )
    }

    @Suppress("ForbiddenComment", "MagicNumber")
    private fun onContinueClicked() {
        (_uiState.value as? Content.Welcome)?.let {
            // TODO: Update this to trigger data migration logic after processing uiState is emitted:
            viewModelScope.launch {
                _uiState.value = it.copy(isProcessing = true)
                // TODO: Remove this temporary delay
                delay(5000)
                _uiState.value = it.copy(isProcessing = false)
                // TODO: navigate to notifications screen
                // TODO: move this to the end of the migration flow
                appPrefsWrapper.isJetpackMigrationFlowCompleted = true
            }
        }
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
                    iconUrl = siteUtilsWrapper.getSiteIconUrlOfResourceSize(
                            site,
                            R.dimen.jp_migration_site_icon_size,
                    ),
            )
        }
    }

    private fun getAvatarUrl(): String {
        return gravatarUtilsWrapper.fixGravatarUrlWithResource(
                accountStore.account?.avatarUrl.orEmpty(),
                R.dimen.jp_migration_user_avatar_size,
        )
    }

    sealed class UiState {
        object Loading : UiState()

        sealed class Content(
            val screenIconRes: Int,
            val title: UiString,
            val subtitle: UiString,
            val message: UiString,
            open val primaryActionButton: ActionButton,
            open val secondaryActionButton: ActionButton,
        ) : UiState() {
            data class Welcome(
                val userAvatarUrl: String = "",
                val isProcessing: Boolean = false,
                val sites: List<SiteListItemUiState>,
                override val primaryActionButton: ActionButton,
                override val secondaryActionButton: ActionButton,
            ) : Content(
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
