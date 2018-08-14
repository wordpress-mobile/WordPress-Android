package org.wordpress.android.ui.reader.viewmodels

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.ViewModel
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.news.NewsItem
import org.wordpress.android.ui.news.NewsManager
import javax.inject.Inject

class ReaderPostListViewModel @Inject constructor(
    private val newsManager: NewsManager
) : ViewModel() {
    private val newsItemSource = newsManager.newsItemSource()
    private val _newsItemSourceMediator = MediatorLiveData<NewsItem>()
    val newsItem: LiveData<NewsItem>
        get() = _newsItemSourceMediator

    private lateinit var initialTag: ReaderTag
    private var isStarted = false

    fun start(tag: ReaderTag) {
        if (isStarted) {
            return
        }
        initialTag = tag
        onTagChanged(tag)
        newsManager.pull()
        isStarted = true
    }

    fun onTagChanged(tag: ReaderTag) {
        // show the card only when the initial tag is selected in the filter
        if (tag == initialTag) {
            _newsItemSourceMediator.addSource(newsItemSource) { _newsItemSourceMediator.value = it }
        } else {
            _newsItemSourceMediator.removeSource(newsItemSource)
            _newsItemSourceMediator.value = null
        }
    }

    fun onDismissClicked(item: NewsItem) {
        newsManager.dismiss(item)
    }

    override fun onCleared() {
        super.onCleared()
        newsManager.stop()
    }
}
