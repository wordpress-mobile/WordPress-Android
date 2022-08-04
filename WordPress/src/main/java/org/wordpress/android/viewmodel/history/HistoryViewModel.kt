package org.wordpress.android.viewmodel.history

import android.text.TextUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.revisions.RevisionModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchRevisionsPayload
import org.wordpress.android.fluxc.store.PostStore.OnRevisionsFetched
import org.wordpress.android.models.Person
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.history.HistoryListItem
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.ui.people.utils.PeopleUtils
import org.wordpress.android.ui.people.utils.PeopleUtils.FetchUsersCallback
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import org.wordpress.android.viewmodel.helpers.ConnectionStatus
import org.wordpress.android.viewmodel.helpers.ConnectionStatus.AVAILABLE
import javax.inject.Inject
import javax.inject.Named

class HistoryViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val networkUtils: NetworkUtilsWrapper,
    private val postStore: PostStore,
    @Named(UI_THREAD) uiDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val connectionStatus: LiveData<ConnectionStatus>
) : ScopedViewModel(uiDispatcher) {
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

    private val _showDialog = SingleLiveEvent<ShowDialogEvent>()
    val showDialog: LiveData<ShowDialogEvent>
        get() = _showDialog

    private var isStarted = false

    private val revisionsList: MutableList<Revision> = mutableListOf()
    private lateinit var site: SiteModel

    private val _post = MutableLiveData<PostModel?>()
    val post: LiveData<PostModel?> = _post

    private val lifecycleOwner = object : LifecycleOwner {
        val lifecycleRegistry = LifecycleRegistry(this)
        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }
    }

    init {
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.CREATED
        dispatcher.register(this)
    }

    fun create(localPostId: Int, site: SiteModel) {
        if (isStarted) {
            return
        }
        isStarted = true
        this.site = site
        connectionStatus.observe(lifecycleOwner, Observer {
            if (it == AVAILABLE) {
                fetchRevisions()
            }
        })
        launch {
            val post: PostModel? = withContext(bgDispatcher) {
                postStore.getPostByLocalPostId(localPostId)
            }

            revisionsList.clear()
            _revisions.value = emptyList()

            this@HistoryViewModel._post.value = post

            fetchRevisions()

            lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.STARTED
        }
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

        if (revisionAuthorsId.isNotEmpty()) {
            fetchRevisionAuthorDetails(revisionAuthorsId)
        }
    }

    private fun fetchRevisionAuthorDetails(authorsId: List<String>) {
        PeopleUtils.fetchRevisionAuthorsDetails(site, authorsId, object : FetchUsersCallback {
            override fun onSuccess(peopleList: List<Person>, isEndOfList: Boolean) {
                val existingRevisions = _revisions.value ?: return
                val updatedRevisions = mutableListOf<HistoryListItem>()
                revisionsList.clear()

                existingRevisions.forEach { existingRevision ->
                    var mutableRevision = existingRevision

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
        val post = this.post.value

        if (post != null) {
            _listStatus.value = HistoryListStatus.FETCHING
            val payload = FetchRevisionsPayload(post, site)
            dispatcher.dispatch(PostActionBuilder.newFetchRevisionsAction(payload))
        } else {
            _listStatus.value = HistoryListStatus.DONE
            createRevisionsList(emptyList())
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
            val footer = if (post.value?.isPage == true) {
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
        lifecycleOwner.lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onCleared()
    }

    fun onItemClicked(item: HistoryListItem) {
        if (item is Revision) {
            _showDialog.value = ShowDialogEvent(item, revisionsList)
        }
    }

    fun onPullToRefresh() {
        fetchRevisions()
    }

    private fun saveRevisionsToLocalDB(post: PostModel, revisions: List<RevisionModel>) {
        revisions.forEach {
            postStore.setLocalRevision(it, site, post)
        }
    }

    private fun removeRevisionsFromLocalDB(post: PostModel) {
        postStore.deleteLocalRevisionOfAPostOrPage(post)
    }

    data class ShowDialogEvent(val historyListItem: HistoryListItem, val revisionsList: List<Revision>)

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
            removeRevisionsFromLocalDB(event.post)
            saveRevisionsToLocalDB(event.post, event.revisionsModel.revisions)
        }
    }
}
