package org.wordpress.android.ui.reader.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagList
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import org.wordpress.android.ui.news.NewsTracker
import org.wordpress.android.ui.news.NewsTracker.NewsCardOrigin.READER
import org.wordpress.android.ui.news.NewsTrackerHelper
import org.wordpress.android.ui.news.NewsViewHolder.NewsCardListener
import org.wordpress.android.viewmodel.Event
import javax.inject.Inject

/**
 * This ViewModel encapsulates logic related to NewsCard. It's a card shown in order to promote a new app feature.
 *
 * The card is shown on the first tab which is selected when the user opens the Reader.
 * The logic when and for how long is the card shown is encapsulated in the NewsManager.
 */
class NewsCardViewModel @Inject constructor(
    private val newsTracker: NewsTracker,
    private val newsTrackerHelper: NewsTrackerHelper,
    private val newsManager: NewsManager
) : ViewModel() {
    /**
     * First tag for which the news card was shown.
     */
    private var initialTag: ReaderTag? = null
    private var selectedTag: ReaderTag? = null

    private val onTagChanged: Observer<NewsItem?> = Observer { _newsItemSourceMediator.value = it }

    private val newsItemSource = newsManager.newsItemSource()
    private val _newsItemSourceMediator = MediatorLiveData<NewsItem>()

    private var started: Boolean = false
    private val _uiState = MutableLiveData<ReaderUiState>()
    val uiState: LiveData<ReaderUiState> = _uiState

    private val _openUrlEvent = MutableLiveData<Event<String>>()
    val openUrlEvent: LiveData<Event<String>> = _openUrlEvent

    val newsCardListener: NewsCardListener = object : NewsCardListener {
        override fun onItemShown(item: NewsItem) {
            onNewsCardShown(item)
        }

        override fun onItemClicked(item: NewsItem) {
            onNewsCardExtendedInfoRequested(item)
            _openUrlEvent.value = Event(item.actionUrl)
        }

        override fun onDismissClicked(item: NewsItem) {
            onNewsCardDismissed(item)
        }
    }

    fun start() {
        if (started) return
        started = true
        newsManager.pull()
    }

    fun onTagChanged(tag: ReaderTag?) {
        selectedTag = tag
        newsTrackerHelper.reset()
        tag?.let { newTag ->
            // show the card only when the initial tag is selected in the filter
            if (initialTag == null || newTag == initialTag) {
                _newsItemSourceMediator.addSource(newsItemSource, onTagChanged)
            } else {
                _newsItemSourceMediator.removeSource(newsItemSource)
                _newsItemSourceMediator.value = null
            }
        }
    }

    fun getNewsDataSource(): LiveData<NewsItem> {
        return _newsItemSourceMediator
    }

    private fun onNewsCardShown(item: NewsItem) {
        initialTag = selectedTag
        if (newsTrackerHelper.shouldTrackNewsCardShown(item.version)) {
            newsTracker.trackNewsCardShown(READER, item.version)
            newsTrackerHelper.itemTracked(item.version)
        }
        newsManager.cardShown(item)
    }

    private fun onNewsCardDismissed(item: NewsItem) {
        newsTracker.trackNewsCardDismissed(READER, item.version)
        newsManager.dismiss(item)
    }

    private fun onNewsCardExtendedInfoRequested(item: NewsItem) {
        newsTracker.trackNewsCardExtendedInfoRequested(READER, item.version)
    }

    override fun onCleared() {
        super.onCleared()
        newsManager.stop()
    }

    data class ReaderUiState(val tabTitles: List<String>, val readerTagList: ReaderTagList)
}
