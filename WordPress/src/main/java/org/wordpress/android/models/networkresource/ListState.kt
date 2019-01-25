package org.wordpress.android.models.networkresource

import android.support.annotation.StringRes
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Init
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.models.networkresource.ListState.Ready
import org.wordpress.android.models.networkresource.ListState.Success

/**
 * ListState aims to give a highly structured and easy to use way to manage any list's state.
 *
 * There are 5 different states: [Init], [Ready], [Success], [Loading], [Error]. Check out their documentation to see
 * how each state behaves.
 *
 * @property data is initialized depending on each state and once initialized it can not be altered. In [Ready] and
 * [Success] states, it'll be passed as a parameter. In [Loading] and [Error] states, it'll be initialized from the
 * previous state to make sure the access to the data is not lost. In [Init], an empty list will be passed.
 * In situations where the data needs to be changed outside of a fetch, [transform] can be used
 * to get a new instance by using a transform function.
 */
sealed class ListState<T>(val data: List<T>) {
    /**
     * In some situations the underlying data might change outside of a fetch. Adding a new item, removing one,
     * a single item getting updated would be some typical examples. Since the [data] property can not be altered
     * directly, which is by design, we need a different way to update it.
     *
     * This method can be used to handle any transformation easily while preserving the current state. Any function
     * that takes a [List] and returns a new one can be used as the transformation.
     *
     * @return a new ListState instance that has the transformed data while preserving the state
     */
    abstract fun transform(transformFunc: (List<T>) -> List<T>): ListState<T>

    /**
     * Helper function for checking whether the first page is being loaded. It can be used to either show or hide a
     * [android.support.v4.widget.SwipeRefreshLayout].
     */
    fun isFetchingFirstPage(): Boolean = if (this is Loading) !loadingMore else false

    /**
     * Helper function for checking whether more data is being loaded. It can be used to either show or hide a
     * [android.widget.ProgressBar] for instance, at the bottom of a screen.
     */
    fun isLoadingMore(): Boolean = (this as? Loading)?.loadingMore == true

    /**
     * Helper function to check whether a fetch should occur. If the state is [Loading] fetch is not allowed. Otherwise,
     * the first page can be fetched at any time. Loading more data is only allowed if it's specifically flagged to be
     * possible in [Success] state.
     *
     * @param loadMore should be passed to indicate what kind of fetch is intended: first page or load more
     */
    fun shouldFetch(loadMore: Boolean): Boolean = when (this) {
        is Init -> false // Not ready yet
        is Loading -> false // Already fetching
        is Success -> if (loadMore) canLoadMore else true // Trying to load more or refreshing
        else -> !loadMore // First page can be fetched since we are not fetching anything else
    }

    /**
     * This is the state each object should be created in. In this state [data] would be empty and [shouldFetch] will
     * return `false` with the assumption that the caller will need to get ready before fetch can happen. A typical
     * example would be to initialize a resource as a property and then [Ready] it after the necessary setup, such as
     * getting the `SiteModel` from a `Store`.
     */
    class Init<T> : ListState<T>(ArrayList()) {
        override fun transform(transformFunc: (List<T>) -> List<T>) = this
    }

    /**
     * Ready state signifies that this resource can start being used.
     *
     * @param data This is one of 2 places where the data can be directly passed in. In most cases, it will be set
     * using the cached version of the data, for example from its `Store`.
     */
    class Ready<T>(data: List<T>) : ListState<T>(data) {
        override fun transform(transformFunc: (List<T>) -> List<T>) = Ready(transformFunc(data))
    }

    /**
     * This state means that a network request has been started to fetch either the first page or more data.
     *
     * @param data can not be passed directly to [Loading] state as it's prevented by a private constructor.
     * It's initialized either from the previous state or from the transformed data using
     * [ListState.transform].
     *
     * @param loadingMore flag is used to indicate whether the first page or more data is being fetched. It's default
     * value is `false` which should be useful in situations where pagination is not available.
     */
    class Loading<T> private constructor(data: List<T>, val loadingMore: Boolean) : ListState<T>(data) {
        constructor(previous: ListState<T>, loadingMore: Boolean = false)
                : this(previous.data, loadingMore)

        override fun transform(transformFunc: (List<T>) -> List<T>) =
                Loading(transformFunc(data), loadingMore)
    }

    /** This state means that at least one fetch has successfully completed.
     *
     * @param data This is the second and final state where the data can be passed in directly.
     *
     * @param canLoadMore For resources where pagination is available, this flag can be used to indicate if more data
     * can be fetched. It's default value is `false` which should be useful in situations where pagination is not
     * available.
     */
    class Success<T>(data: List<T>, val canLoadMore: Boolean = false) : ListState<T>(data) {
        override fun transform(transformFunc: (List<T>) -> List<T>) =
                Success(transformFunc(data), canLoadMore)
    }

    /**
     * This state means that at least one fetch has resulted in error.
     *
     * @param data can not be passed directly to [Error] state as it's prevented by a private constructor.
     * It's initialized either from the previous state or from the transformed data using
     * [ListState.transform].
     *
     * @param errorMessage will be the error string received from the API.
     * @param errorMessageResId can be used to propagate error resourceIds from ViewModels to Views.
     */
    class Error<T> private constructor(
        data: List<T>,
        val errorMessage: String? = null,
        @StringRes val errorMessageResId: Int? = null
    ) : ListState<T>(data) {
        constructor(previous: ListState<T>, errorMessage: String? = null, @StringRes errorMessageResId: Int? = null)
                : this(previous.data, errorMessage, errorMessageResId)

        override fun transform(transformFunc: (List<T>) -> List<T>) =
                Error(transformFunc(data), errorMessage, errorMessageResId)
    }
}
