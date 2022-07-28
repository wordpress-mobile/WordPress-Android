package org.wordpress.android.ui.sitecreation.usecases

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.ThemeActionBuilder
import org.wordpress.android.fluxc.store.ThemeStore
import org.wordpress.android.fluxc.store.ThemeStore.FetchStarterDesignsPayload
import org.wordpress.android.fluxc.store.ThemeStore.OnStarterDesignsFetched
import org.wordpress.android.ui.sitecreation.theme.SiteDesignPickerDimensionProvider
import org.wordpress.android.util.config.BetaSiteDesignsFeatureConfig
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FetchHomePageLayoutsUseCase @Inject constructor(
    val dispatcher: Dispatcher,
    @Suppress("unused") val themeStore: ThemeStore,
    private val thumbDimensionProvider: SiteDesignPickerDimensionProvider,
    private val betaSiteDesigns: BetaSiteDesignsFeatureConfig
) {
    enum class GROUP(val key: String) {
        STABLE("stable"),
        BETA("beta")
    }
    private var continuation: Continuation<OnStarterDesignsFetched>? = null

    @Suppress("UseCheckOrError")
    suspend fun fetchStarterDesigns(): OnStarterDesignsFetched {
        if (continuation != null) {
            throw IllegalStateException("Fetch already in progress.")
        }
        val payload = if (betaSiteDesigns.isEnabled()) FetchStarterDesignsPayload(
                thumbDimensionProvider.previewWidth.toFloat(),
                thumbDimensionProvider.previewHeight.toFloat(),
                thumbDimensionProvider.scale.toFloat(),
                GROUP.STABLE.key, GROUP.BETA.key
        ) else FetchStarterDesignsPayload(
                thumbDimensionProvider.previewWidth.toFloat(),
                thumbDimensionProvider.previewHeight.toFloat(),
                thumbDimensionProvider.scale.toFloat()
        )
        return suspendCoroutine { cont ->
            continuation = cont
            dispatcher.dispatch(ThemeActionBuilder.newFetchStarterDesignsAction(payload))
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onStarterDesignsFetched(event: OnStarterDesignsFetched) {
        continuation?.resume(event)
        continuation = null
    }
}
