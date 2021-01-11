package org.wordpress.android.ui.jetpack

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.model.JetpackCapability
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchJetpackCapabilitiesPayload
import org.wordpress.android.fluxc.store.SiteStore.OnJetpackCapabilitiesFetched
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val MAX_CACHE_VALIDITY = 1000 * 60 * 15L // 15 minutes

class JetpackCapabilitiesUseCase @Inject constructor(
    @Suppress("unused") private val siteStore: SiteStore,
    private val dispatcher: Dispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val currentDateProvider: CurrentTimeProvider,
    @param:Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) {
    private var continuation: Continuation<OnJetpackCapabilitiesFetched>? = null

    suspend fun getOrFetchJetpackCapabilities(remoteSiteId: Long): List<JetpackCapability> {
        return if (hasValidCache(remoteSiteId)) {
            getCachedJetpackCapabilities(remoteSiteId)
        } else {
            fetchJetpackCapabilities(remoteSiteId)
        }
    }

    private suspend fun hasValidCache(remoteSiteId: Long): Boolean {
        return withContext(bgDispatcher) {
            val lastUpdated = appPrefsWrapper.getSiteJetpackCapabilitiesLastUpdated(remoteSiteId)
            lastUpdated > currentDateProvider.currentDate.time - MAX_CACHE_VALIDITY
        }
    }

    private suspend fun getCachedJetpackCapabilities(remoteSiteId: Long): List<JetpackCapability> {
        return withContext(bgDispatcher) { appPrefsWrapper.getSiteJetpackCapabilities(remoteSiteId) }
    }

    private suspend fun fetchJetpackCapabilities(remoteSiteId: Long): List<JetpackCapability> {
        return withContext(bgDispatcher) {
            if (continuation != null) {
                throw IllegalStateException("Request already in progress.")
            }

            dispatcher.register(this@JetpackCapabilitiesUseCase)
            val response = suspendCoroutine<OnJetpackCapabilitiesFetched> { cont ->
                val payload = FetchJetpackCapabilitiesPayload(remoteSiteId)
                continuation = cont
                dispatcher.dispatch(SiteActionBuilder.newFetchJetpackCapabilitiesAction(payload))
            }

            val capabilities: List<JetpackCapability> = response.capabilities ?: listOf()
            if (!response.isError) {
                updateCache(remoteSiteId, capabilities)
            }
            return@withContext capabilities
        }
    }

    private fun updateCache(
        remoteSiteId: Long,
        capabilities: List<JetpackCapability>
    ) {
        appPrefsWrapper.setSiteJetpackCapabilities(
                remoteSiteId,
                capabilities
        )
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    @SuppressWarnings("unused")
    fun onJetpackCapabilitiesFetched(event: OnJetpackCapabilitiesFetched) {
        dispatcher.unregister(this@JetpackCapabilitiesUseCase)
        continuation?.resume(event)
        continuation = null
    }
}
