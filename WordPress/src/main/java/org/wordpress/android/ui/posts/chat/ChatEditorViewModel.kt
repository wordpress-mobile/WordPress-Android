package org.wordpress.android.ui.posts.chat

import androidx.lifecycle.LiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.editor.media.GetMediaModelUseCase
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.lang.StringBuilder
import javax.inject.Inject
import javax.inject.Named

private const val HEADER_LENGTH_THRESHOLD = 4

class ChatEditorViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val mediaModelUseCase: GetMediaModelUseCase,
    @Named(BG_THREAD) val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private val _onNewContent = SingleLiveEvent<String>()
    val onNewContent: LiveData<String> = _onNewContent

    private val _onClearInput = SingleLiveEvent<Unit>()
    val onClearInput: LiveData<Unit> = _onClearInput

    private val _onAttachRequest = SingleLiveEvent<Unit>()
    val onAttachRequest: LiveData<Unit> = _onAttachRequest

    private val _onMediaListChanged = SingleLiveEvent<List<MediaModel>>()
    val onMediaListChanged: LiveData<List<MediaModel>> = _onMediaListChanged

    fun onSendButtonPressed(text: String) {
        _onClearInput.call()
        _onNewContent.value = getContent(text, _onMediaListChanged.value ?: listOf())
        _onMediaListChanged.value = listOf()
    }

    fun onAttachButtonPressed() {
        if (_onMediaListChanged.value.isNullOrEmpty()) {
            _onAttachRequest.call()
        } else {
            _onMediaListChanged.value = listOf()
        }
    }

    fun onMediaSelected(ids: List<Long>) {
        launch {
            val siteId = appPrefsWrapper.getSelectedSite()
            val site = siteStore.getSiteByLocalId(siteId)
            val media = mediaModelUseCase.loadMediaByRemoteId(site, ids)
            withContext(mainDispatcher) {
                _onMediaListChanged.value = media
            }
        }
    }

    private fun getContent(text: String, mediaList: List<MediaModel>): String {
        val content = StringBuilder()
        if (mediaList.isEmpty()) {
            setTextContent(text, content)
        } else { // Media attached
            val images = mediaList.filter { !it.isVideo }
            val videos = mediaList.filter { it.isVideo }
            when (images.size) {
                0 -> setTextContent(text, content)
                1 -> {
                    if (text.isNotEmpty()) {
                        content.append(mediaTextBlock(images[0], text))
                    } else {
                        content.append(imageBlock(images[0]))
                    }
                }
                else -> {
                    if (text.isNotEmpty()) {
                        setTextContent(text, content)
                    }
                    content.append(galleryBlock(images))
                }
            }
            videos.forEach { video ->
                content.append(videoBlock(video))
            }
        }
        return content.toString()
    }

    private fun setTextContent(text: String, content: StringBuilder) {
        if (text.wordCount > HEADER_LENGTH_THRESHOLD) {
            content.append(text.paragraphBlock)
        } else {
            content.append(text.headingBlock)
        }
    }
}
