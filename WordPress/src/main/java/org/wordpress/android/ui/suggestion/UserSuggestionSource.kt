package org.wordpress.android.ui.suggestion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject

class UserSuggestionSource @Inject constructor(
    context: Context,
    override val site: SiteModel,
    private val eventBusWrapper: EventBusWrapper
) : SuggestionSource {
    private val connectionManager = SuggestionServiceConnectionManager(context, site.siteId)

    private val _suggestions = MutableLiveData<List<Suggestion>>()
    override val suggestions: LiveData<List<Suggestion>> = _suggestions

    init {
        _suggestions.postValue(savedSuggestions())
        connectionManager.bindToService()
        eventBusWrapper.register(this)
    }

    private fun savedSuggestions() =
            Suggestion.fromUserSuggestions(
                    UserSuggestionTable.getSuggestionsForSite(site.siteId)
            )

    override fun refreshSuggestions() {
        connectionManager.apply {
            unbindFromService()
            bindToService()
        }
    }

    @Subscribe
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        if (event.mRemoteBlogId == site.siteId) {
            _suggestions.postValue(savedSuggestions())
        }
    }

    override fun onCleared() {
        eventBusWrapper.unregister(this)
        connectionManager.unbindFromService()
    }
}
