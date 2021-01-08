package org.wordpress.android.ui.reader.subfilter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.datasets.ReaderBlogTable
import org.wordpress.android.datasets.ReaderTagTable
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.Organization
import org.wordpress.android.ui.Organization.NO_ORGANIZATION
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic.UpdateTask
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Site
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.SiteAll
import org.wordpress.android.ui.reader.subfilter.SubfilterListItem.Tag
import org.wordpress.android.ui.reader.subfilter.SubfilterVisibilityUiModel.SubfilterHide
import org.wordpress.android.ui.reader.subfilter.SubfilterVisibilityUiModel.SubfilterShow
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.EventBusWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import java.util.EnumSet
import javax.inject.Inject
import javax.inject.Named

class SubFilterSharedViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val eventBusWrapper: EventBusWrapper,
    private val accountStore: AccountStore
) : ScopedViewModel(bgDispatcher) {
    private val _subFilters = MutableLiveData<List<SubfilterListItem>>()
    val subFilters: LiveData<List<SubfilterListItem>> = _subFilters

    private val _changeBottomSheetVisibility = MutableLiveData<Event<SubfilterVisibilityUiModel>>()
    val changeBottomSheetVisibility: LiveData<Event<SubfilterVisibilityUiModel>> = _changeBottomSheetVisibility

    private val _updateTagsAndSites = MutableLiveData<Event<EnumSet<UpdateTask>>>()
    val updateTagsAndSites: LiveData<Event<EnumSet<UpdateTask>>> = _updateTagsAndSites

    private val _onSubfilterClicked = MutableLiveData<HashMap<Organization, Event<SubfilterListItem>>>()
    val onSubfilterClicked: LiveData<HashMap<Organization, Event<SubfilterListItem>>> = _onSubfilterClicked

    private val _filtersMatchCount = MutableLiveData<HashMap<SubfilterCategory, Int>>()
    val filtersMatchCount: LiveData<HashMap<SubfilterCategory, Int>> = _filtersMatchCount

    private val _bottomSheetEmptyViewAction = MutableLiveData<Event<ActionType>>()
    val bottomSheetEmptyViewAction: LiveData<Event<ActionType>> = _bottomSheetEmptyViewAction

    private var isStarted = false

    private var lastKnownUserId: Long? = null
    private var lastTokenAvailableStatus: Boolean? = null

    fun start() {
        if (isStarted) {
            return
        }

        isStarted = true

        _filtersMatchCount.value = hashMapOf()
        _onSubfilterClicked.value = hashMapOf()

        eventBusWrapper.register(this)
    }

    fun loadSubFilters() {
        launch {
            val filterList = ArrayList<SubfilterListItem>()

            if (accountStore.hasAccessToken()) {
                // Filtering Discover out
                val followedBlogs = ReaderBlogTable.getFollowedBlogs().let { blogList ->
                    blogList.filter { blog ->
                        !(blog.url.startsWith("https://discover.wordpress.com"))
                    }
                }

                filterList.addAll(
                        followedBlogs.map { blog ->
                            Site(
                                    onClickAction = ::subfilterClickedAction,
                                    blog = blog,
                                    isSelected = false,
                                    organization = Organization.fromOrgId(blog.organizationId)
                            )
                        }
                )
            }

            val tags = ReaderTagTable.getFollowedTags()

            for (tag in tags) {
                filterList.add(Tag(
                        onClickAction = ::subfilterClickedAction,
                        tag = tag,
                        isSelected = false,
                        organization = NO_ORGANIZATION
                ))
            }

            withContext(mainDispatcher) {
                _subFilters.value = filterList
            }
        }
    }

    private fun updateOnSubfilterClickedMap(filter: SubfilterListItem) {
        val clickedMap = _onSubfilterClicked.value
        clickedMap?.put(filter.organization, Event(filter))
        _onSubfilterClicked.postValue(clickedMap)
    }

    private fun subfilterClickedAction(filter: SubfilterListItem) {
        _changeBottomSheetVisibility.postValue(Event(SubfilterHide))
        updateOnSubfilterClickedMap(filter)
    }

    fun setSubfilterFromTag(tag: ReaderTag, organization: Organization) {
        updateOnSubfilterClickedMap(Tag(
                onClickAction = ::subfilterClickedAction,
                tag = tag,
                isSelected = true,
                organization = organization))
    }

    fun setDefaultSubfilter(organization: Organization) {
        updateOnSubfilterClickedMap(SiteAll(
                        isSelected = true,
                        organization = organization))
    }

    fun onSubFiltersListButtonClicked(organization: Organization) {
        _updateTagsAndSites.value = Event(
                EnumSet.of(
                        UpdateTask.TAGS,
                        UpdateTask.FOLLOWED_BLOGS
                ))
        _changeBottomSheetVisibility.value = Event(SubfilterShow(organization))
    }

    fun onBottomSheetCancelled() {
        _changeBottomSheetVisibility.value = Event(SubfilterHide)
    }

    fun onSubfilterPageUpdated(category: SubfilterCategory, count: Int) {
        val currentValue = _filtersMatchCount.value

        currentValue?.let {
            it.put(category, count)
        }

        _filtersMatchCount.postValue(currentValue)
    }




    fun onUserComesToReader(organization: Organization) {
        // TODO I think this method could be simplified, I don't think we need to store the data in the sharedPref
        if (lastKnownUserId == null) {
            lastKnownUserId = appPrefsWrapper.getLastReaderKnownUserId()
        }

        if (lastTokenAvailableStatus == null) {
            lastTokenAvailableStatus = appPrefsWrapper.getLastReaderKnownAccessTokenStatus()
        }

        val userIdChanged = accountStore.hasAccessToken() && accountStore.account != null &&
                accountStore.account.userId != lastKnownUserId
        val accessTokenStatusChanged = accountStore.hasAccessToken() != lastTokenAvailableStatus

        if (userIdChanged) {
            lastKnownUserId = accountStore.account.userId
            appPrefsWrapper.setLastReaderKnownUserId(accountStore.account.userId)
        }

        if (accessTokenStatusChanged) {
            lastTokenAvailableStatus = accountStore.hasAccessToken()
            appPrefsWrapper.setLastReaderKnownAccessTokenStatus(accountStore.hasAccessToken())
        }

        if (userIdChanged || accessTokenStatusChanged) {
            _updateTagsAndSites.value = Event(EnumSet.of(
                    UpdateTask.TAGS,
                    UpdateTask.FOLLOWED_BLOGS
            ))

            setDefaultSubfilter(organization)
        }
    }

    fun onBottomSheetActionClicked(action: ActionType) {
        _changeBottomSheetVisibility.postValue(Event(SubfilterHide))
        _bottomSheetEmptyViewAction.postValue(Event(action))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedTagsChanged) {
        AppLog.d(T.READER, "Subfilter bottom sheet > followed tags changed")
        loadSubFilters()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: ReaderEvents.FollowedBlogsChanged) {
        AppLog.d(T.READER, "Subfilter bottom sheet > followed blogs changed")
        loadSubFilters()
    }

    override fun onCleared() {
        super.onCleared()
        eventBusWrapper.unregister(this)
    }
}
