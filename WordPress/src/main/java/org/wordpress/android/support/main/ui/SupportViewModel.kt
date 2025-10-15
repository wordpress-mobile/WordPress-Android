package org.wordpress.android.support.main.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class SupportViewModel @Inject constructor(
    private val accountStore: AccountStore,
) : ViewModel() {

    data class UserInfo(
        val userName: String = "",
        val userEmail: String = "",
        val avatarUrl: String? = null
    )

    private val _userInfo = MutableStateFlow(UserInfo())
    val userInfo: StateFlow<UserInfo> = _userInfo.asStateFlow()

    fun init() {
        val account = accountStore.account
        _userInfo.value = UserInfo(
            userName = account.displayName.ifEmpty { account.userName },
            userEmail = account.email,
            avatarUrl = account.avatarUrl.takeIf { it.isNotEmpty() }
        )
    }

    fun onHelpCenterClick() {
        // TODO: Navigate to Help Center
    }

    fun onAskTheBotsClick() {
        // TODO: Navigate to AI Bot Support
    }

    fun onAskHappinessEngineersClick() {
        // TODO: Navigate to Happiness Engineers contact
    }

    fun onApplicationLogsClick() {
        // TODO: Navigate to Application Logs
    }
}
