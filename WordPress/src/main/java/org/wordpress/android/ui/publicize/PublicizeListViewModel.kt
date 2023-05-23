package org.wordpress.android.ui.publicize

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class PublicizeListViewModel @Inject constructor(
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
) : ScopedViewModel(bgDispatcher) {
    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState> = _uiState

    private val _actionEvents = SingleLiveEvent<ActionEvent>()
    val actionEvents: LiveData<ActionEvent> = _actionEvents

    private var shouldUpdateServices = true
    private var twitterPublicizeService: PublicizeService? = null

    fun onSiteAvailable(siteModel: SiteModel) {
        if (!shouldUpdateServices) {
            return
        }
        shouldUpdateServices = false
        val siteId = siteModel.siteId
        updateList(siteId)
    }

    fun onTwitterDeprecationNoticeItemClick() {
        twitterPublicizeService?.let {
            _actionEvents.value = ActionEvent.OpenServiceDetails(it)
        }
    }

    private fun updateList(siteId: Long) {
        publicizeUpdateServicesV2.updateServices(
            siteId = siteId,
            success = { services ->
                services.forEach {
                    if (it.id == PublicizeService.TWITTER_SERVICE_ID) {
                        twitterPublicizeService = it
                    }
                }
                updateConnections(siteId)
            },
            failure = {
                AppLog.e(AppLog.T.SHARING, "Error updating publicize services", it)
                updateConnections(siteId)
            }
        )
    }

    private fun updateConnections(siteId: Long) {
        publicizeUpdateServicesV2.updateConnections(
            siteId = siteId,
            success = { connections ->
                val isTwitterDeprecated = twitterPublicizeService?.status == PublicizeService.Status.UNSUPPORTED
                val twitterConnection = connections.find {
                    it.service == PublicizeService.TWITTER_SERVICE_ID
                }
                val isConnectionAvailable = connections.getServiceConnectionsForUser(
                    accountStore.account.userId, twitterPublicizeService?.id
                ).isNotEmpty()
                if (isTwitterDeprecated && twitterConnection != null && isConnectionAvailable) {
                    showTwitterDeprecationNotice(twitterConnection)
                }
                eventBusWrapper.post(PublicizeEvents.ConnectionsChanged())
            },
            failure = { AppLog.e(AppLog.T.SHARING, "Error updating publicize connections", it) }
        )
    }

    private fun showTwitterDeprecationNotice(twitterConnection: PublicizeConnection) {
        _uiState.value = UIState.ShowTwitterDeprecationNotice(
            title = R.string.sharing_twitter_deprecation_notice_title,
            serviceName = twitterConnection.label,
            description = R.string.sharing_twitter_deprecation_notice_description,
            findOutMore = R.string.sharing_twitter_deprecation_notice_find_out_more,
            findOutMoreUrl = TWITTER_DEPRECATION_FIND_OUT_MORE_URL,
            iconUrl = twitterPublicizeService?.iconUrl.orEmpty(),
            connectedUser = twitterConnection.externalDisplayName,
        )
    }

    sealed class UIState {
        data class ShowTwitterDeprecationNotice(
            @StringRes val title: Int,
            val serviceName: String,
            @StringRes val description: Int,
            @StringRes val findOutMore: Int,
            val findOutMoreUrl: String,
            val iconUrl: String,
            val connectedUser: String,
        ) : UIState()
    }

    sealed class ActionEvent {
        data class OpenServiceDetails(val service: PublicizeService) : ActionEvent()
    }
}
