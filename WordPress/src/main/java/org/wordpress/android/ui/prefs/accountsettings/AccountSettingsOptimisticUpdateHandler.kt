package org.wordpress.android.ui.prefs.accountsettings

import org.wordpress.android.ui.prefs.accountsettings.AccountSettingsViewModel.AccountSettingsUiState
import javax.inject.Inject

const val EMAIL_PREFERENCE_KEY = "EMAIL_PREFERENCE_KEY"
const val PRIMARYSITE_PREFERENCE_KEY = "PRIMARYSITE_PREFERENCE_KEY"
const val WEBADDRESS_PREFERENCE_KEY = "WEBADDRESS_PREFERENCE_KEY"

/**
 * In AccountSettings, when the preference is changed by the user, the changed preference will be reflected immediately
 * in the screen even before the preference is actually updated in the server. We are optimistic that preferences will
 * be updated in the server successfully.
 * In case of any failure, we are reverting back the preference change and notifying user with error message.
 * Whenever a request is made for the preference change, optimisticallyChangedPreferenceMap is updated with the
 * preference key and value that has to be optimistically updated to ui state.
 *
 * on calling applyOptimisticallyChangedPreferences function, the preferences that has to be updated
 * optimistically will be updated with the latest value. (ie. last())
 *
 * Once the server returns the response, the preference key and value is removed from the
 * optimisticallyChangedPreferenceMap.
 */
class AccountSettingsOptimisticUpdateHandler @Inject constructor() {
    private val optimisticallyChangedPreferenceMap = mutableMapOf<String, List<String>>()

    fun applyOptimisticallyChangedPreferences(state: AccountSettingsUiState): AccountSettingsUiState {
        var uiState = state
        optimisticallyChangedPreferenceMap.forEach { (key, arrayOfValues) ->
            when (key) {
                EMAIL_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            emailSettingsUiState = state.emailSettingsUiState.copy(
                                    newEmail = arrayOfValues.last(),
                                    hasPendingEmailChange = true
                            )
                    )
                }
                WEBADDRESS_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            webAddressSettingsUiState = state.webAddressSettingsUiState.copy(
                                    webAddress = arrayOfValues.last()
                            )
                    )
                }
                PRIMARYSITE_PREFERENCE_KEY -> {
                    uiState = uiState.copy(
                            primarySiteSettingsUiState = state.primarySiteSettingsUiState.copy(
                                    primarySite = state.primarySiteSettingsUiState.sites?.lastOrNull {
                                        it.siteId == arrayOfValues.last()
                                                .toLong()
                                    })
                    )
                }
            }
        }
        return uiState
    }

    fun update(preferenceKey: String, value: String): () -> Unit = {
        optimisticallyChangedPreferenceMap[preferenceKey] = optimisticallyChangedPreferenceMap.getOrDefault(
                preferenceKey,
                listOf()
        ).plus(value)
    }

    fun removeFirstChange(preferenceKey: String): () -> Unit = removeFirstChangeLambda@{
        val preferenceValue = optimisticallyChangedPreferenceMap
                .getOrDefault(preferenceKey, listOf())
        if (preferenceValue.isEmpty()) return@removeFirstChangeLambda
        if (preferenceValue.size == 1) {
            optimisticallyChangedPreferenceMap.remove(preferenceKey)
        } else {
            optimisticallyChangedPreferenceMap[preferenceKey] = preferenceValue.drop(1)
        }
    }
}
