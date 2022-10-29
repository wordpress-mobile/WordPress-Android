package org.wordpress.android.ui.main.jetpack.migration

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.wordpress.android.R
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.StepUiState.Welcome
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

@HiltViewModel
class JetpackMigrationViewModel @Inject constructor(
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onAccountInfoLoaded() {
        // TODO update UI when account info is loaded to db
    }

    fun onSiteListLoaded() {
        _uiState.value = _uiState.value.copy(
                stepState = Welcome(
                        sites = getSiteList(),
                ),
        )
    }

    private fun getSiteList(): List<SiteListItemUiState> {
        return listOf() // TODO load site list and map to UI model
    }

    data class UiState(
        val stepState: StepUiState? = null,
    )

    sealed class StepUiState(
        val screenIconRes: Int,
        val title: UiString,
    ) {
        data class Welcome(
            val sites: List<SiteListItemUiState>,
        ) : StepUiState(
                screenIconRes = R.drawable.ic_wordpress_jetpack_logo,
                title = UiStringRes(R.string.jp_migration_welcome_title),
        )
    }

    data class SiteListItemUiState(
        val id: Long,
        val name: String,
        val url: String,
        val iconUrl: String,
    )
}
