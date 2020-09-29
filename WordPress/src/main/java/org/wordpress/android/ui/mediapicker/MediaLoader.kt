package org.wordpress.android.ui.mediapicker

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction.ClearFilter
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction.Filter
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction.NextPage
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction.Refresh
import org.wordpress.android.ui.mediapicker.MediaLoader.LoadAction.Start
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.util.LocaleManagerWrapper

data class MediaLoader(
    private val mediaSource: MediaSource,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val allowedTypes: Set<MediaType>
) {
    suspend fun loadMedia(actions: Channel<LoadAction>): Flow<DomainModel> {
        return flow {
            var state = DomainModel()
            for (loadAction in actions) {
                when (loadAction) {
                    is Start -> {
                        if (state.domainItems.isEmpty() || state.error != null) {
                            state = buildDomainModel(mediaSource.load(allowedTypes), state)
                            emit(state)
                        }
                    }
                    is Refresh -> {
                        if (loadAction.forced || state.domainItems.isEmpty()) {
                            emit(state.copy(isLoading = true))
                            state = buildDomainModel(mediaSource.load(allowedTypes, forced = loadAction.forced), state)
                            emit(state)
                        }
                    }
                    is NextPage -> {
                        val load = mediaSource.load(mediaTypes = allowedTypes, loadMore = true)
                        state = buildDomainModel(load, state)
                        emit(state)
                    }
                    is Filter -> {
                        state = state.copy(
                                filter = loadAction.filter,
                                domainItems = mediaSource.get(allowedTypes, loadAction.filter)
                        )
                        emit(state)
                    }
                    is ClearFilter -> {
                        state = state.copy(filter = null, domainItems = mediaSource.get(allowedTypes, null))
                        emit(state)
                    }
                }
            }
        }
    }

    private suspend fun buildDomainModel(
        partialResult: MediaLoadingResult,
        state: DomainModel
    ): DomainModel {
        return when (partialResult) {
            is Success -> state.copy(
                    isLoading = false,
                    error = null,
                    hasMore = partialResult.hasMore,
                    domainItems = mediaSource.get(allowedTypes, state.filter)
            )
            is Failure -> state.copy(isLoading = false, error = partialResult.message)
        }
    }

    sealed class LoadAction {
        data class Start(val filter: String? = null) : LoadAction()
        data class Refresh(val forced: Boolean) : LoadAction()
        data class Filter(val filter: String) : LoadAction()
        object NextPage : LoadAction()
        object ClearFilter : LoadAction()
    }

    data class DomainModel(
        val domainItems: List<MediaItem> = listOf(),
        val error: String? = null,
        val hasMore: Boolean = false,
        val isFilteredResult: Boolean = false,
        val filter: String? = null,
        val isLoading: Boolean = false
    )
}
