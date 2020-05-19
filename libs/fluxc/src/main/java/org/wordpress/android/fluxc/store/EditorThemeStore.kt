package org.wordpress.android.fluxc.store

import android.os.Bundle
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.EditorThemeAction
import org.wordpress.android.fluxc.action.EditorThemeAction.FETCH_EDITOR_THEME
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditorThemeStore
@Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    class OnEditorThemeChanged(
        val editorThemes: Map<String, Bundle>
    ) : Store.OnChanged<OnChangedError>() {
    }

    fun getEditorThemeForSite(site: SiteModel): Bundle {
        val stubbedColors = Bundle()

        val accent = Bundle()
        accent.putString("name", "Accent Color")
        accent.putString("slug", "accent")
        accent.putString("color", "#cd2653")

        val primary = Bundle()
        primary.putString("name", "Primary")
        primary.putString("slug", "primary")
        primary.putString("color", "#000000")

        val secondary = Bundle()
        secondary.putString("name", "Secondary")
        secondary.putString("slug", "secondary")
        secondary.putString("color", "#6d6d6d")

        val subtle = Bundle()
        subtle.putString("name", "Subtle Background")
        subtle.putString("slug", "subtle-background")
        subtle.putString("color", "#dcd7ca")

        val background = Bundle()
        background.putString("name", "Background Color")
        background.putString("slug", "background")
        background.putString("color", "#f5efe0")

        val colors = ArrayList<Bundle>()
        colors.add(accent)
        colors.add(primary)
        colors.add(secondary)
        colors.add(subtle)
        colors.add(background)

        val gradient = Bundle()
        gradient.putString("name", "Blue to Purple")
        gradient.putString("slug", "blue-to-purple")
        gradient.putString(
                "gradient",
                "linear-gradient(135deg,rgba(6,147,227,1) 0%,rgb(155,81,224) 100%)"
        )
        val gradients = ArrayList<Bundle>()
        gradients.add(gradient)

        stubbedColors.putSerializable("colors", colors)
        stubbedColors.putSerializable("gradients", gradients)
        return stubbedColors
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EditorThemeAction ?: return
        when (actionType) {
            FETCH_EDITOR_THEME -> {
            coroutineEngine.launch(AppLog.T.API, this, TransactionsStore::class.java.simpleName + ": On FETCH_EDITOR_THEME") {
                handleFetchEditorTheme()
            }
        }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, TransactionsStore::class.java.simpleName + " onRegister")
    }

    private fun handleFetchEditorTheme() {
    }
}
