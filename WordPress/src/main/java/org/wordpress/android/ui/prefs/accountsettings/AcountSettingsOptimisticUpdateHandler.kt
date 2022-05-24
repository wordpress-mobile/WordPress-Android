package org.wordpress.android.ui.prefs.accountsettings

import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountSettingsUiState
import javax.inject.Inject

const val EMAIL_PREFERENCE_KEY = "EMAIL_PREFERENCE_KEY"
const val PRIMARYSITE_PREFERENCE_KEY = "PRIMARYSITE_PREFERENCE_KEY"
const val WEBADDRESS_PREFERENCE_KEY = "WEBADDRESS_PREFERENCE_KEY"
class AcountSettingsOptimisticUpdateHandler @Inject constructor() {

    private val optimisticallyChangedPreferenceMap = mutableMapOf<String, List<String>>()

    fun applyOptimisticallyChangedPreferences(state: AccountSettingsUiState): AccountSettingsUiState {
        var uiState = state
        optimisticallyChangedPreferenceMap.forEach { key, arrayOfValues ->
            when (key) {
                EMAIL_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            emailSettingsUiState = state.emailSettingsUiState.copy(
                                    newEmail = arrayOfValues.first(),
                                    hasPendingEmailChange = true
                            )
                    )
                }
                WEBADDRESS_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            webAddressSettingsUiState = state.webAddressSettingsUiState.copy(
                                    webAddress = arrayOfValues.first()
                            )
                    )
                }
                PRIMARYSITE_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            primarySiteSettingsUiState = state.primarySiteSettingsUiState.copy(
                                    primarySite = state.primarySiteSettingsUiState.sites?.firstOrNull {
                                        it.siteId == arrayOfValues.first()
                                                .toLong()
                                    })
                    )
                }
            }
        }
        return uiState
    }

    fun update(preferenceKey: String, value: String): () -> Unit? = {
        optimisticallyChangedPreferenceMap[preferenceKey] = optimisticallyChangedPreferenceMap.getOrDefault(
                preferenceKey,
                listOf()
        ).plus(value)
        null
    }

    fun removeFirstChange(preferenceKey: String): () -> Unit? = {
        val preferenceValue = optimisticallyChangedPreferenceMap.getOrDefault(preferenceKey, listOf())
        if (preferenceValue?.size == 1) {
            optimisticallyChangedPreferenceMap.remove(preferenceKey)
        } else {
            optimisticallyChangedPreferenceMap.put(preferenceKey, preferenceValue?.drop(1))
        }
        null
    }
}
