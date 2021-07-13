package org.wordpress.android.fluxc.store

import com.google.gson.Gson
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.EditorThemeAction
import org.wordpress.android.fluxc.action.EditorThemeAction.FETCH_EDITOR_THEME
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.BlockEditorSettings
import org.wordpress.android.fluxc.model.EditorTheme
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.persistence.EditorThemeSqlUtils
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.helpers.Version
import javax.inject.Inject
import javax.inject.Singleton

private const val THEME_REQUEST_PATH = "/wp/v2/themes?status=active"
private const val EDITOR_SETTINGS_REQUEST_PATH = "wp-block-editor/v1/settings?context=mobile"
private const val EDITOR_SETTINGS_WP_VERSION = "5.8"

@Singleton
class EditorThemeStore
@Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private val editorThemeSqlUtils = EditorThemeSqlUtils()

    class FetchEditorThemePayload @JvmOverloads constructor(val site: SiteModel, val gssEnabled: Boolean = false) :
            Payload<BaseNetworkError>()

    data class OnEditorThemeChanged(
        val editorTheme: EditorTheme?,
        val siteId: Int,
        val causeOfChange: EditorThemeAction
    ) : Store.OnChanged<EditorThemeError>() {
        constructor(error: EditorThemeError, causeOfChange: EditorThemeAction) :
                this(editorTheme = null, siteId = -1, causeOfChange = causeOfChange) {
            this.error = error
        }
    }

    class EditorThemeError(var message: String? = null) : OnChangedError

    fun getEditorThemeForSite(site: SiteModel): EditorTheme? {
        return editorThemeSqlUtils.getEditorThemeForSite(site)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EditorThemeAction ?: return
        when (actionType) {
            FETCH_EDITOR_THEME -> {
                coroutineEngine.launch(
                        AppLog.T.API,
                        this,
                        EditorThemeStore::class.java.simpleName + ": On FETCH_EDITOR_THEME"
                ) {
                    val payload = action.payload as FetchEditorThemePayload
                    if (editorSettingsAvailable(payload.site, payload.gssEnabled)) {
                        fetchEditorSettings(payload.site, actionType)
                    } else {
                        fetchEditorTheme(payload.site, actionType)
                    }
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, EditorThemeStore::class.java.simpleName + " onRegister")
    }

    private suspend fun fetchEditorTheme(site: SiteModel, action: EditorThemeAction) {
        val response = reactNativeStore.executeRequest(site, THEME_REQUEST_PATH, false)

        when (response) {
            is Success -> {
                val noThemeError = OnEditorThemeChanged(EditorThemeError("Response does not contain a theme"), action)
                if (response.result == null || !response.result.isJsonArray) {
                    emitChange(noThemeError)
                    return
                }

                val responseTheme = response.result.asJsonArray.firstOrNull()
                if (responseTheme == null) {
                    emitChange(noThemeError)
                    return
                }

                val newTheme = Gson().fromJson(responseTheme, EditorTheme::class.java)
                val existingTheme = editorThemeSqlUtils.getEditorThemeForSite(site)
                if (newTheme != existingTheme) {
                    editorThemeSqlUtils.replaceEditorThemeForSite(site, newTheme)
                    val onChanged = OnEditorThemeChanged(newTheme, site.id, action)
                    emitChange(onChanged)
                }
            }
            is Error -> {
                val onChanged = OnEditorThemeChanged(EditorThemeError(response.error.message), action)
                emitChange(onChanged)
            }
        }
    }

    private suspend fun fetchEditorSettings(site: SiteModel, action: EditorThemeAction) {
        val response = reactNativeStore.executeRequest(site, EDITOR_SETTINGS_REQUEST_PATH, false)

        when (response) {
            is Success -> {
                response.handleFetchEditorSettingsResponse(site, action)
            }
            is Error -> {
                if (response.error.type == NOT_FOUND) {
                    /**
                     * We tried the editor settings call first but since that failed we fall back to the themes endpoint
                     * since the user may not have the gutenberg plugin installed.
                     */
                    fetchEditorTheme(site, action)
                } else {
                    response.handleFetchEditorSettingsResponse(action)
                }
            }
        }
    }

    private fun ReactNativeFetchResponse.Success.handleFetchEditorSettingsResponse(
        site: SiteModel,
        action: EditorThemeAction
    ) {
        val noGssError = OnEditorThemeChanged(EditorThemeError("Response does not contain GSS"), action)
        if (result == null || !result.isJsonObject) {
            emitChange(noGssError)
            return
        }

        val responseTheme = result.asJsonObject
        if (responseTheme == null) {
            emitChange(noGssError)
            return
        }

        val blockEditorSettings = Gson().fromJson(responseTheme, BlockEditorSettings::class.java)
        val newTheme = EditorTheme(blockEditorSettings)
        val existingTheme = editorThemeSqlUtils.getEditorThemeForSite(site)
        if (newTheme != existingTheme) {
            editorThemeSqlUtils.replaceEditorThemeForSite(site, newTheme)
            val onChanged = OnEditorThemeChanged(newTheme, site.id, action)
            emitChange(onChanged)
        }
    }

    private fun ReactNativeFetchResponse.Error.handleFetchEditorSettingsResponse(action: EditorThemeAction) {
        val onChanged = OnEditorThemeChanged(EditorThemeError(error.message), action)
        emitChange(onChanged)
    }

    private fun editorSettingsAvailable(site: SiteModel, gssEnabled: Boolean) =
            gssEnabled && site.hasRequiredWordPressVersion(EDITOR_SETTINGS_WP_VERSION)

    /**
     * Checks if the [SiteModel.getSoftwareVersion] is higher or equal to the [requiredVersion]
     *
     * Note: At this point semantic version information (alpha, beta etc) is stripped since it
     * is not supported by our [Version] utility
     *
     * @param requiredVersion the required WordPress version
     * @return true if the check is met
     */
    private fun SiteModel.hasRequiredWordPressVersion(requiredVersion: String) = try {
        val version = if (softwareVersion.contains("-")) {
            // strip semantic versioning information (alpha, beta etc)
            softwareVersion.substringBefore("-")
        } else softwareVersion
        Version(version) >= Version(requiredVersion)
    } catch (e: IllegalArgumentException) {
        false // if version parsing fails return false
    }
}
