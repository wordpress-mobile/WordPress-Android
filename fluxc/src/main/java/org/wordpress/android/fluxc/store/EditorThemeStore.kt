package org.wordpress.android.fluxc.store

import com.google.gson.Gson
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.EditorThemeAction
import org.wordpress.android.fluxc.action.EditorThemeAction.FETCH_EDITOR_THEME
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton
import java.net.URI

@Singleton
class EditorThemeStore
@Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
): Store(dispatcher) {
    private val THEME_REQUEST_PATH = "/wp/v2/themes?status=active"

    var editorThemes = HashMap<String, EditorTheme>()

    class FetchEditorThemePayload(val site: SiteModel): Payload<BaseNetworkError>() {
        constructor(
            error: BaseNetworkError,
            site: SiteModel
        ) : this(site = site) {
            this.error = error
        }
    }

    data class OnEditorThemeChanged(
        val editorThemes: Map<String, EditorTheme>,
        var causeOfChange: EditorThemeAction
    ) : Store.OnChanged<EditorThemeError>() {
        constructor(error: EditorThemeError, causeOfChange: EditorThemeAction):
                this(editorThemes = HashMap<String, EditorTheme>(), causeOfChange = causeOfChange) {
            this.error = error
        }
    }
    class EditorThemeError(var message: String? = null) : Store.OnChangedError

    fun getEditorThemeForSite(site: SiteModel): EditorTheme? {
        return null
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EditorThemeAction ?: return
        when (actionType) {
            FETCH_EDITOR_THEME -> {
            coroutineEngine.launch(AppLog.T.API, this, TransactionsStore::class.java.simpleName + ": On FETCH_EDITOR_THEME") {
                handleFetchEditorTheme((action.payload as FetchEditorThemePayload).site, actionType)
            }
        }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, TransactionsStore::class.java.simpleName + " onRegister")
    }

    fun editorThemeKeyForSite(site: SiteModel): String {
        return URI(site.url).getHost()
    }

    private suspend fun handleFetchEditorTheme(site: SiteModel, action: EditorThemeAction) {
        val response = reactNativeStore.executeRequest(site, THEME_REQUEST_PATH)

        when (response) {
            is Success -> {
                val responseTheme = response.result.asJsonArray.first() ?: return
                val key = editorThemeKeyForSite(site)
                val newTheme = Gson().fromJson(responseTheme, EditorTheme::class.java)

                if (newTheme != null) {
                    editorThemes.set(key, newTheme)
                    val onChanged = OnEditorThemeChanged(editorThemes, action)
                    emitChange(onChanged)
                }
            }
            is Error -> {
                val onChanged = OnEditorThemeChanged(EditorThemeError(response.error.message), action)
                emitChange(onChanged)
            }
        }
    }
}
