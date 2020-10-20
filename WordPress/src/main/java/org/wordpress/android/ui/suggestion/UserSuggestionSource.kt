package org.wordpress.android.ui.suggestion

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.wordpress.android.datasets.UserSuggestionTable
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.suggestion.service.SuggestionEvents.SuggestionNameListUpdated
import org.wordpress.android.ui.suggestion.util.SuggestionServiceConnectionManager
import org.wordpress.android.util.EventBusWrapper
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class UserSuggestionSource @Inject constructor(
    context: Context,
    override val site: SiteModel,
    private val eventBusWrapper: EventBusWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : SuggestionSource, CoroutineScope {
    override val coroutineContext: CoroutineContext = bgDispatcher + Job()
    private val connectionManager = SuggestionServiceConnectionManager(context, site.siteId)

    private val _suggestions = MutableLiveData<List<Suggestion>>()
    override val suggestions: LiveData<List<Suggestion>> = _suggestions

    init {
        postSavedSuggestions()
        connectionManager.bindToService()
        eventBusWrapper.register(this)
    }

    private fun postSavedSuggestions() {
        launch {
            val suggestions = Suggestion.fromUserSuggestions(
                    UserSuggestionTable.getSuggestionsForSite(site.siteId)
            )
            _suggestions.postValue(suggestions)
        }
    }

    override fun refreshSuggestions() {
        connectionManager.apply {
            unbindFromService()
            bindToService()
        }
    }

    @Subscribe
    fun onEventMainThread(event: SuggestionNameListUpdated) {
        if (event.mRemoteBlogId == site.siteId) {
            postSavedSuggestions()
        }
    }

    override fun onCleared() {
        eventBusWrapper.unregister(this)
        connectionManager.unbindFromService()
    }
}
