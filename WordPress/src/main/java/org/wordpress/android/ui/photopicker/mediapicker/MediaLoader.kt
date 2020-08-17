package org.wordpress.android.ui.photopicker.mediapicker

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.LoadAction.Filter
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.LoadAction.NextPage
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.LoadAction.Refresh
import org.wordpress.android.ui.photopicker.mediapicker.MediaLoader.LoadAction.Start
import org.wordpress.android.ui.photopicker.mediapicker.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.photopicker.mediapicker.MediaSource.MediaLoadingResult.Success

class MediaLoader(private val mediaSource: MediaSource) {
    suspend fun loadMedia(actions: Channel<LoadAction>): Flow<DomainModel> {
        return flow {
            var state = DomainState()
            for (loadAction in actions) {
                when (loadAction) {
                    is Start -> {
                        if (state.mediaTypes != loadAction.mediaTypes || state.items.isEmpty() || state.error != null) {
                            state = refreshData(state, loadAction.mediaTypes)
                        }
                    }
                    is Refresh -> {
                        state.mediaTypes?.let { mediaTypes ->
                            state = refreshData(state, mediaTypes)
                        }
                    }
                    is NextPage -> {
                        state.mediaTypes?.let { mediaTypes ->
                            state = when (val mediaLoadingResult = mediaSource.load(
                                    mediaTypes,
                                    offset = state.items.size
                            )) {
                                is Success -> {
                                    state.copy(
                                            items = state.items + mediaLoadingResult.mediaItems,
                                            hasMore = mediaLoadingResult.hasMore,
                                            error = null
                                    )
                                }
                                is Failure -> {
                                    state.copy(
                                            error = mediaLoadingResult.message
                                    )
                                }
                            }
                        }
                    }
                    is Filter -> {
                        state = state.copy(filter = loadAction.filter)
                    }
                }
                if (state.isNotInitialState()) {
                    emit(buildDomainModel(state))
                }
            }
        }
    }

    private suspend fun refreshData(state: DomainState, mediaTypes: Set<MediaType>): DomainState {
        return when (val mediaLoadingResult = mediaSource.load(mediaTypes)) {
            is Success -> {
                state.copy(
                        items = mediaLoadingResult.mediaItems,
                        hasMore = mediaLoadingResult.hasMore,
                        mediaTypes = mediaTypes,
                        error = null
                )
            }
            is Failure -> {
                state.copy(
                        error = mediaLoadingResult.message,
                        mediaTypes = mediaTypes,
                        hasMore = false
                )
            }
        }
    }

    private fun buildDomainModel(state: DomainState): DomainModel {
        return if (!state.filter.isNullOrEmpty()) {
            DomainModel(state.items.filter { it.name?.contains(state.filter) == true }, state.error, state.hasMore)
        } else {
            DomainModel(state.items, state.error, state.hasMore)
        }
    }

    sealed class LoadAction {
        data class Start(val mediaTypes: Set<MediaType>) : LoadAction()
        object Refresh : LoadAction()
        data class Filter(val filter: String) : LoadAction()
        object NextPage : LoadAction()
    }

    data class DomainModel(val domainItems: List<MediaItem>, val error: String? = null, val hasMore: Boolean = false)

    private data class DomainState(
        val mediaTypes: Set<MediaType>? = null,
        val items: List<MediaItem> = listOf(),
        val hasMore: Boolean = false,
        val filter: String? = null,
        val error: String? = null
    ) {
        fun isNotInitialState(): Boolean = mediaTypes != null
    }
}
