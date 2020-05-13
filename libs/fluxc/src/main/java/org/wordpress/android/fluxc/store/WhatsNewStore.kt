package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnounceModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.whatsnew.WhatsNewRestClient
import org.wordpress.android.fluxc.store.Store.OnChangedError
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog.T
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhatsNewStore @Inject constructor(
    private val whatsNewRestClient: WhatsNewRestClient,
    private val coroutineEngine: CoroutineEngine
) {
//    @Subscribe(threadMode = ThreadMode.ASYNC)
//    override fun onAction(action: Action<*>) {
//        val actionType = action.type as? WhatsNewAction ?: return
//        when (actionType) {
//            FETCH_WHATS_NEW -> {
//                val versionCode = (action.payload as WhatsNewFetchPayload).versionCode
//                GlobalScope.launch(coroutineContext) { emitChange(fetchWhatsNew(versionCode)) }
//            }
//        }
//    }

    suspend fun fetchWhatsNew(versionCode: String) = coroutineEngine.withDefaultContext(T.API, this, "fetchWhatsNew") {
        val fetchedWhatsNewPayload = whatsNewRestClient.fetchWhatsNew(versionCode)

        return@withDefaultContext if (!fetchedWhatsNewPayload.isError) {
            OnWhatsNewFetched(fetchedWhatsNewPayload.whatsNewItems)
        } else {
            OnWhatsNewFetched(
                    fetchError = WhatsNewFetchError(GENERIC_ERROR, fetchedWhatsNewPayload.error.message)
            )
        }
    }

    class WhatsNewFetchPayload(
        val versionCode: String
    ) : Payload<BaseNetworkError>()

    class WhatsNewFetchedPayload(
        val whatsNewItems: List<WhatsNewAnnounceModel>? = null
    ) : Payload<BaseNetworkError>()

    data class OnWhatsNewFetched(
        val whatsNewItems: List<WhatsNewAnnounceModel>? = null,
        val fetchError: WhatsNewFetchError? = null
    ) : Store.OnChanged<WhatsNewFetchError>() {
        init {
            // we allow setting error from constructor, so it will be a part of data class
            // and used during comparison, so we can test error events
            this.error = fetchError
        }
    }

    data class WhatsNewFetchError(
        val type: WhatsNewErrorType,
        val message: String = ""
    ) : OnChangedError

    enum class WhatsNewErrorType {
        GENERIC_ERROR
    }
}
