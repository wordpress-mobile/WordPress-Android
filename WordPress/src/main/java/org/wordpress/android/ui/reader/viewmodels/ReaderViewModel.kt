package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.ReaderEvents
import org.wordpress.android.ui.reader.usecases.LoadReaderTabsUseCase
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.distinct
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

const val UPDATE_TAGS_THRESHOLD = 1000 * 60 * 60

typealias TabPosition = Int

class ReaderViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dateProvider: DateProvider,
    private val loadReaderTabsUseCase: LoadReaderTabsUseCase
) : ScopedViewModel(mainDispatcher) {
    private var initialized: Boolean = false

    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState.distinct()

    private val _updateTags = MutableLiveData<Event<Unit>>()
    val updateTags: LiveData<Event<Unit>> = _updateTags

    private val _selectTab = MutableLiveData<Event<TabPosition>>()
    val selectTab: LiveData<Event<TabPosition>> = _selectTab

    init {
        EventBus.getDefault().register(this)
    }

    fun start() {
        if (tagsRequireUpdate()) _updateTags.value = Event(Unit)
        if (initialized) return
        loadTabs()
    }

    private fun loadTabs() {
        launch {
            val tagList = loadReaderTabsUseCase.loadTabs()
            if (tagList.isNotEmpty()) {
                _uiState.value = ReaderUiState(
                        tagList.map { it.tagTitle },
                        tagList
                )
                if (!initialized) {
                    initialized = true
                    restoreTabSelection(tagList)
                }
            }
        }
    }

    private suspend fun restoreTabSelection(tagList: ReaderTagList) {
        withContext(bgDispatcher) {
            appPrefsWrapper.getReaderTag()?.let {
                val index = tagList.indexOf(it)
                if (index != -1) {
                    _selectTab.postValue(Event(index))
                }
            }
        }
    }

    fun onTagChanged(selectedTag: ReaderTag?) {
        // Store most recently selected tab so we can restore the selection after restart
        appPrefsWrapper.setReaderTag(selectedTag)
    }

    data class ReaderUiState(val tabTitles: List<String>, val readerTagList: ReaderTagList)

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
    }

    private fun tagsRequireUpdate(): Boolean {
        val lastUpdated = appPrefsWrapper.readerTagsUpdatedTimestamp
        val now = dateProvider.getCurrentDate().time
        return now - lastUpdated > UPDATE_TAGS_THRESHOLD
    }

    fun selectedTabChange(tag: ReaderTag) {
        uiState.value?.let {
            val position = it.readerTagList.indexOfTagName(tag.tagSlug)
            _selectTab.postValue(Event(position))
        }
    }

    @Subscribe(threadMode = MAIN)
    fun onTagsUpdated(event: ReaderEvents.FollowedTagsChanged) {
        loadTabs()
    }
}

