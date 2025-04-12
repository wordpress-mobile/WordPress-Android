package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.EditorSettingsAction
import org.wordpress.android.fluxc.action.EditorSettingsAction.FETCH_EDITOR_SETTINGS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.EditorSettings
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.EditorSettingsSqlUtils
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

private const val EDITOR_SETTINGS_REQUEST_PATH = "wp-block-editor/v1/settings"

@Singleton
class EditorSettingsStore @Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private val editorSettingsSqlUtils = EditorSettingsSqlUtils()

    class FetchEditorSettingsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    data class OnEditorSettingsChanged(
        val editorSettings: EditorSettings?,
        val siteId: Int,
        val causeOfChange: EditorSettingsAction,
        val isFromCache: Boolean = false
    ) : Store.OnChanged<EditorSettingsError>() {
        constructor(error: EditorSettingsError, causeOfChange: EditorSettingsAction) :
                this(editorSettings = null, siteId = -1, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    class EditorSettingsError(var message: String? = null) : OnChangedError

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EditorSettingsAction ?: return
        when (actionType) {
            FETCH_EDITOR_SETTINGS -> {
                coroutineEngine.launch(
                    AppLog.T.API,
                    this,
                    EditorSettingsStore::class.java.simpleName + ": On FETCH_EDITOR_SETTINGS"
                ) {
                    val payload = action.payload as FetchEditorSettingsPayload
                    fetchEditorSettings(payload.site, actionType)
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, EditorSettingsStore::class.java.simpleName + " onRegister")
    }

    private suspend fun fetchEditorSettings(site: SiteModel, action: EditorSettingsAction) {
        // First emit cached data if available
        val cachedSettings = editorSettingsSqlUtils.getEditorSettingsForSite(site)
        if (cachedSettings != null) {
            emitChange(OnEditorSettingsChanged(cachedSettings, site.id, action, isFromCache = true))
        }

        // Then fetch fresh data
        val response = reactNativeStore.executeGetRequest(site, EDITOR_SETTINGS_REQUEST_PATH, false)

        when (response) {
            is Success -> {
                if (response.result == null || !response.result.isJsonObject) {
                    emitChange(OnEditorSettingsChanged(
                        EditorSettingsError("Response does not contain editor settings"),
                        action
                    ))
                    return
                }

                val editorSettings = EditorSettings(response.result.asJsonObject)
                // Update cache
                editorSettingsSqlUtils.replaceEditorSettingsForSite(site, editorSettings)

                // Only emit change if the data is different from cache
                if (cachedSettings != editorSettings) {
                    val onChanged = OnEditorSettingsChanged(editorSettings, site.id, action)
                    emitChange(onChanged)
                }
            }
            is Error -> {
                if (cachedSettings != null) {
                    val onChanged = OnEditorSettingsChanged(cachedSettings, site.id, action, isFromCache = true)
                    emitChange(onChanged)
                } else {
                    val onChanged = OnEditorSettingsChanged(
                        EditorSettingsError(response.error.message),
                        action
                    )
                    emitChange(onChanged)
                }
            }
        }
    }
}
