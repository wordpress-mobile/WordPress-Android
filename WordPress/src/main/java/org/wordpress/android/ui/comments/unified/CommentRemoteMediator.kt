package org.wordpress.android.ui.comments.unified

import android.util.Log
import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import kotlinx.coroutines.delay
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.util.NetworkUtilsWrapper
import javax.inject.Inject

@OptIn(ExperimentalPagingApi::class)
class CommentRemoteMediator @Inject constructor(
    private val networkUtilsWrapper: NetworkUtilsWrapper
) : RemoteMediator<Int, CommentModel>() {
    override suspend fun initialize(): InitializeAction {
        return InitializeAction.LAUNCH_INITIAL_REFRESH
    }

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CommentModel>
    ): MediatorResult {
        when (loadType) {
            LoadType.REFRESH -> null
            // In this example, you never need to prepend, since REFRESH
            // will always load the first page in the list. Immediately
            // return, reporting end of pagination.
            LoadType.PREPEND -> return MediatorResult.Success(
                    endOfPaginationReached = true
            )
            // Get the last User object id for the next RemoteKey.
            LoadType.APPEND -> {
                val lastItem = state.lastItemOrNull()

                // You must explicitly check if the last item is null when
                // appending, since passing null to networkService is only
                // valid for initial load. If lastItem is null it means no
                // items were loaded after the initial REFRESH and there are
                // no more items to load.
                if (lastItem?.id == 149) {
                    return MediatorResult.Success(
                            endOfPaginationReached = true
                    )
                }
            }
        }
        Log.v("Comments", "UC: Remote Data Refresh Requested: $loadType")
        // do some network stuff and populate DB
        delay(5000) // synthetic delay

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return MediatorResult.Error(Error("Network Unavailable"))
        }

        Log.v("Comments", "UC: Remote Data Loaded")
        notifyListeners()
        return MediatorResult.Success(
                endOfPaginationReached = false
        )
    }

    private var listeners = arrayListOf<() -> Unit>()

    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    private fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        ArrayList(listeners).forEach {
            it()
            removeListener(it)
        }
    }
}
