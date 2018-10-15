package org.wordpress.android.viewmodel.history

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.revisions.RevisionModel
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsPayload
import org.wordpress.android.fluxc.store.PostStore.OnRevisionsFetched
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope
) : ViewModel() {
    enum class HistoryListStatus {
        DONE,
        ERROR,
        FETCHING
    }

    private val _listStatus = MutableLiveData<HistoryListStatus>()
    val eventListStatus: LiveData<HistoryListStatus>
        get() = _listStatus

    private val _showLoadDialog = SingleLiveEvent<HistoryListItem>()
    val showLoadDialog: LiveData<HistoryListItem>
        get() = _showLoadDialog

    private val _showSnackbarMessage = SingleLiveEvent<String>()
    val showSnackbarMessage: LiveData<String>
        get() = _showSnackbarMessage

    private val _revisions = MutableLiveData<List<HistoryListItem>>()
    val revisions: LiveData<List<HistoryListItem>>
        get() = _revisions

    private var isStarted = false

    lateinit var post: PostModel
    lateinit var site: SiteModel

    init {
        dispatcher.register(this)
    }

    fun create(post: PostModel, site: SiteModel) {
        if (isStarted) {
            return
        }

        this.post = post
        this.site = site

        fetchRevisions()

        isStarted = true
    }

    private fun createRevisionsList(revisions: List<RevisionModel>) {
        val items = mutableListOf<HistoryListItem>()

        revisions.forEach {
            val item = HistoryListItem.Revision(it)
            val last = items.lastOrNull() as? Revision

            if (item.formattedDate != last?.formattedDate) {
                items.add(HistoryListItem.Header(item.formattedDate))
            }

            items.add(item)
        }

        if (revisions.isNotEmpty()) {
            val last = items.last() as Revision
            val footer = if (post.isPage) {
                resourceProvider.getString(R.string.history_footer_page, last.formattedDate, last.formattedTime)
            } else {
                resourceProvider.getString(R.string.history_footer_post, last.formattedDate, last.formattedTime)
            }
            items.add(HistoryListItem.Footer(footer))
        }

        _revisions.value = items
    }

    private fun fetchRevisions() {
        _listStatus.value = HistoryListStatus.FETCHING
        val payload = FetchRevisionsPayload(post, site)
        uiScope.launch {
            dispatcher.dispatch(PostActionBuilder.newFetchRevisionsAction(payload))
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    fun onItemClicked(item: HistoryListItem) {
        if (item is HistoryListItem.Revision) {
            _showLoadDialog.value = item
        }
    }

    fun onPullToRefresh() {
        fetchRevisions()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onRevisionsFetched(event: OnRevisionsFetched) {
        if (event.isError) {
            _listStatus.value = HistoryListStatus.ERROR
            AppLog.e(T.API, "An error occurred while fetching History revisions")
        } else {
            _listStatus.value = HistoryListStatus.DONE
            createRevisionsList(event.revisionsModel.revisions)
        }
    }
}
