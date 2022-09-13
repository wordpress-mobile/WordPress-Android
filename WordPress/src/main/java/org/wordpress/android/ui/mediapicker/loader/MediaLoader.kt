package org.wordpress.android.ui.mediapicker.loader

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.wordpress.android.ui.mediapicker.MediaItem
import org.wordpress.android.ui.mediapicker.MediaType
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.DomainModel.EmptyState
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.ClearFilter
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.Filter
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.NextPage
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.Refresh
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.Retry
import org.wordpress.android.ui.mediapicker.loader.MediaLoader.LoadAction.Start
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Empty
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Failure
import org.wordpress.android.ui.mediapicker.loader.MediaSource.MediaLoadingResult.Success
import org.wordpress.android.ui.utils.UiString

data class MediaLoader(
    private val mediaSource: MediaSource
) {
    suspend fun loadMedia(actions: Channel<LoadAction>): Flow<DomainModel> {
        return flow {
            val mediaTypes = (mediaSource as? MediaSourceWithTypes)?.mediaTypes
            var state = DomainModel(mediaTypes = mediaTypes)
            var lastPerformedAction: LoadAction? = null
            for (loadAction in actions) {
                val currentAction = if (loadAction is Retry) {
                    lastPerformedAction ?: loadAction
                } else {
                    lastPerformedAction = loadAction
                    loadAction
                }

                if (currentAction !is NextPage) {
                    state = updateState(state.copy(isLoading = true, emptyState = null))
                }
                val updatedState = loadState(currentAction, state)
                if (state != updatedState) {
                    state = updateState(updatedState)
                }
                if (state.isLoading) {
                    state = updateState(state.copy(isLoading = false))
                }
            }
        }
    }

    private suspend fun loadState(
        loadAction: LoadAction,
        state: DomainModel
    ): DomainModel {
        return when (loadAction) {
            is Start -> {
                if (state.domainItems.isEmpty()) {
                    buildDomainModel(
                            mediaSource.load(filter = loadAction.filter),
                            state.copy(filter = loadAction.filter)
                    )
                } else {
                    state
                }
            }
            is Refresh -> {
                if (loadAction.forced || state.domainItems.isEmpty()) {
                    buildDomainModel(
                            mediaSource.load(
                                    filter = state.filter,
                                    forced = loadAction.forced
                            ), state
                    )
                } else {
                    state
                }
            }
            is NextPage -> {
                val load = mediaSource.load(filter = state.filter, loadMore = true)
                buildDomainModel(load, state)
            }
            is Filter -> {
                val load = mediaSource.load(filter = loadAction.filter)
                buildDomainModel(load, state.copy(filter = loadAction.filter))
            }
            is ClearFilter -> {
                val load = mediaSource.load(filter = null)
                buildDomainModel(load, state.copy(filter = null))
            }
            is Retry -> {
                buildDomainModel(mediaSource.load(filter = state.filter), state)
            }
        }
    }

    private suspend fun FlowCollector<DomainModel>.updateState(
        updatedState: DomainModel
    ): DomainModel {
        emit(updatedState)
        return updatedState
    }

    private fun buildDomainModel(
        partialResult: MediaLoadingResult,
        state: DomainModel
    ): DomainModel {
        return when (partialResult) {
            is Success -> state.copy(
                    isLoading = false,
                    hasMore = partialResult.hasMore,
                    domainItems = partialResult.data,
                    emptyState = null
            )
            is Empty -> state.copy(
                    isLoading = false,
                    hasMore = false,
                    domainItems = listOf(),
                    emptyState = EmptyState(
                            partialResult.title,
                            partialResult.htmlSubtitle,
                            partialResult.image,
                            partialResult.bottomImage,
                            partialResult.bottomImageContentDescription,
                            isError = false
                    )
            )
            is Failure -> state.copy(
                    isLoading = false,
                    hasMore = partialResult.data.isNotEmpty(),
                    domainItems = partialResult.data,
                    emptyState = EmptyState(
                            partialResult.title,
                            partialResult.htmlSubtitle,
                            partialResult.image,
                            isError = true
                    )
            )
        }
    }

    sealed class LoadAction {
        data class Start(val filter: String? = null) : LoadAction()
        data class Refresh(val forced: Boolean) : LoadAction()
        data class Filter(val filter: String) : LoadAction()
        object NextPage : LoadAction()
        object ClearFilter : LoadAction()
        object Retry : LoadAction()
    }

    data class DomainModel(
        val domainItems: List<MediaItem> = listOf(),
        val mediaTypes: Set<MediaType>? = null,
        val hasMore: Boolean = false,
        val isFilteredResult: Boolean = false,
        val filter: String? = null,
        val isLoading: Boolean = false,
        val emptyState: EmptyState? = null
    ) {
        data class EmptyState(
            val title: UiString,
            val htmlSubtitle: UiString? = null,
            val image: Int? = null,
            val bottomImage: Int? = null,
            val bottomImageDescription: UiString? = null,
            val isError: Boolean = false
        )
    }
}
