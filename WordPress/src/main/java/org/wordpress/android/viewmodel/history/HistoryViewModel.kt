package org.wordpress.android.viewmodel.history

import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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
import org.wordpress.android.models.Person
import org.wordpress.android.modules.UI_SCOPE
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.ui.people.utils.PeopleUtils
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val networkUtils: NetworkUtilsWrapper,
    @param:Named(UI_SCOPE) private val uiScope: CoroutineScope,
    connectionStatus: LiveData<ConnectionStatus>
) : ViewModel(), LifecycleOwner {
    enum class HistoryListStatus {
        DONE,
        ERROR,
        NO_NETWORK,
        FETCHING
    }

    private val _listStatus = MutableLiveData<HistoryListStatus>()
    val listStatus: LiveData<HistoryListStatus>
        get() = _listStatus

    private val _revisions = MutableLiveData<List<HistoryListItem>>()
    val revisions: LiveData<List<HistoryListItem>>
        get() = _revisions

    private val _showDialog = SingleLiveEvent<HistoryListItem>()
    val showDialog: LiveData<HistoryListItem>
        get() = _showDialog

    private var isStarted = false

    lateinit var revisionsList: ArrayList<Revision>
    lateinit var post: PostModel
    lateinit var site: SiteModel

    private val lifecycleRegistry = LifecycleRegistry(this)
    override fun getLifecycle(): Lifecycle = lifecycleRegistry

    init {
        lifecycleRegistry.markState(Lifecycle.State.CREATED)
        dispatcher.register(this)
        connectionStatus.observe(this, Observer {
            if (it == AVAILABLE) {
                fetchRevisions()
            }
        })
    }

    fun create(post: PostModel, site: SiteModel) {
        if (isStarted) {
            return
        }

        this.revisionsList = ArrayList()
        this.post = post
        this.site = site
        this._revisions.value = emptyList()

        fetchRevisions()

        isStarted = true
        lifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    private fun createRevisionsList(revisions: List<RevisionModel>) {
        var revisionAuthorsId = ArrayList<String>()
        revisions.forEach {
            if (!TextUtils.isEmpty(it.postAuthorId)) {
                revisionAuthorsId.add(it.postAuthorId!!)
            }
        }

        revisionAuthorsId = ArrayList(revisionAuthorsId.distinct())
        _revisions.value = getHistoryListItemsFromRevisionModels(revisions)
        fetchRevisionAuthorDetails(revisionAuthorsId)
    }

    private fun fetchRevisionAuthorDetails(authorsId: List<String>) {
        PeopleUtils.fetchRevisionAuthorsDetails(site, authorsId, object : FetchUsersCallback {
            override fun onSuccess(peopleList: List<Person>, isEndOfList: Boolean) {
                val existingRevisions = _revisions.value ?: return
                val updatedRevisions = mutableListOf<HistoryListItem>()
                revisionsList.clear()

                existingRevisions.forEach { it ->
                    var mutableRevision = it

                    if (mutableRevision is Revision) {
                        // we shouldn't directly update items in MutableLiveData, as they will be updated downstream
                        // and DiffUtil will not catch this change
                        mutableRevision = mutableRevision.copy()

                        val person = peopleList.firstOrNull { it.personID.toString() == mutableRevision.postAuthorId }
                        if (person != null) {
                            mutableRevision.authorAvatarURL = person.avatarUrl
                            mutableRevision.authorDisplayName = person.displayName
                        }

                        revisionsList.add(mutableRevision)
                    }

                    updatedRevisions.add(mutableRevision)
                }

                _revisions.postValue(updatedRevisions)
            }

            override fun onError() {
                AppLog.e(T.API, "Can't fetch details of revision authors")
            }
        })
    }

    private fun fetchRevisions() {
        _listStatus.value = HistoryListStatus.FETCHING
        val payload = FetchRevisionsPayload(post, site)
        uiScope.launch {
            dispatcher.dispatch(PostActionBuilder.newFetchRevisionsAction(payload))
        }
    }

    private fun getHistoryListItemsFromRevisionModels(revisions: List<RevisionModel>): List<HistoryListItem> {
        val items = mutableListOf<HistoryListItem>()

        revisions.forEach {
            val item = Revision(it)
            val last = items.lastOrNull() as? Revision

            if (item.formattedDate != last?.formattedDate) {
                items.add(HistoryListItem.Header(item.formattedDate))
            }

            items.add(item)
            revisionsList.add(item)
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

        return items
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        lifecycleRegistry.markState(Lifecycle.State.DESTROYED)
        super.onCleared()
    }

    fun onItemClicked(item: HistoryListItem) {
        if (item is Revision) {
            _showDialog.value = item
        }
    }

    fun onPullToRefresh() {
        fetchRevisions()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @SuppressWarnings("unused")
    fun onRevisionsFetched(event: OnRevisionsFetched) {
        if (event.isError) {
            AppLog.e(T.API, "An error occurred while fetching History revisions")
            if (networkUtils.isNetworkAvailable()) {
                _listStatus.value = HistoryListStatus.ERROR
            } else {
                _listStatus.value = HistoryListStatus.NO_NETWORK
            }
        } else {
            _listStatus.value = HistoryListStatus.DONE
            createRevisionsList(event.revisionsModel.revisions)
        }
    }
}
