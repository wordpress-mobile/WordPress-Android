package org.wordpress.android.models.networkresource

/**
 * ListNetworkResource aims to give a highly structured and easy to use way to manage any list that's network bound. It
 * was specifically designed to be as simple as possible. In order to utilize it you don't need to understand the inner
 * workings of the class, although it'd be straightforward to do so.
 *
 * There are 5 different states: [Init], [Ready], [Success], [Loading], [Error]. Check out their documentation to see
 * how each state behaves.
 *
 * @property previous is the previous state. There are several use cases for it one of which is calculating the
 * difference in data in [org.wordpress.android.ui.ListDiffCallback]. Another example would be to check the previous
 * [Loading] state to see if the first page or more data was being fetched to show the proper error to the user.
 * In [Init] state this should be `null`, but for every other state a previous one will need to be passed in.
 *
 * @property data is initialized depending on each state and once initialized it can not be altered. In [Ready] and
 * [Success] states, it'll be passed as a parameter. In [Loading] and [Error] states, it'll be initialized from the
 * previous state to make sure the access to the data is not lost. In [Init], an empty list will be passed.
 * In situations where the data needs to be changed outside of a fetch [getTransformedListNetworkResource] can be used
 * to get a new instance by using a transform function.
 */
sealed class ListNetworkResource<T>(val previous: ListNetworkResource<T>?, val data: List<T>) {
    /**
     * In some situations the data might change outside of a fetch for the list. Adding a new item, removing one,
     * changing contents would be some typical examples. In these situations, the current data might need to be
     * transformed.
     *
     * This method can be used to handle any such transformation easily while preserving the current state. Any function
     * that takes a [List] and returns a new one can be used. The only important thing to keep in mind is that, the new
     * instance that'll be returned from this method will have the current resource as its [previous] state. That way
     * there is a continuity to the states and the data difference can be calculated correctly in
     * [org.wordpress.android.ui.ListDiffCallback].
     */
    abstract fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>): ListNetworkResource<T>

    /**
     * Helper function for [Ready] which passes `this` as the previous state.
     *
     * @return a new [ListNetworkResource] instance.
     */
    fun ready(data: List<T>): ListNetworkResource<T> = Ready(this, data)

    /**
     * Helper function for [Success] which passes `this` as the previous state.
     *
     * @return a new [ListNetworkResource] instance.
     */
    fun success(data: List<T>, canLoadMore: Boolean = false) = Success(this, data, canLoadMore)

    /**
     * Helper function for [Loading] which passes `this` as the previous state.
     *
     * @return a new [ListNetworkResource] instance.
     */
    fun loading(loadingMore: Boolean) = Loading(this, loadingMore)

    /**
     * Helper function for [Error] which passes `this` as the previous state.
     *
     * @return a new [ListNetworkResource] instance.
     */
    fun error(errorMessage: String?) = Error(this, errorMessage)

    /**
     * Helper function for checking whether the first page is being loaded. It can be used to either show or hide a
     * [android.support.v4.widget.SwipeRefreshLayout].
     */
    fun isFetchingFirstPage(): Boolean = if (this is Loading) !loadingMore else false

    /**
     * Helper function for checking whether more data is being loaded. It can be used to either show or hide a
     * [android.widget.ProgressBar] such as at the bottom of a screen.
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
     * example would be to initialize a resource as a property and then [ready] it after the necessary setup, such as
     * getting the `SiteModel` from a `Store`.
     */
    class Init<T> : ListNetworkResource<T>(null, ArrayList()) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = this
    }

    /**
     * Ready state signifies that this resource can start being used.
     *
     * @param previous The previous state. In most cases, [Init] state should be passed in for it, however in cases
     * where a reset is necessary such as change to a search term (which invalidates the current data), another state
     * might be passed in for it. See the property explanation in [ListNetworkResource] for more details.
     *
     * @param data This is one of 2 places where the data can be directly passed in. In most cases, it will be set
     * using the cached version of the data, for example from its `Store`.
     *
     * @see ready helper function for easier initialization.
     */
    class Ready<T>(previous: ListNetworkResource<T>, data: List<T>) : ListNetworkResource<T>(previous, data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) = Ready(this, transform(data))
    }

    /**
     * This state means that a network request has been started to fetch either the first page or more data.
     *
     * @param previous The previous state. See the property explanation in [ListNetworkResource] for more details.
     *
     * @param data can not be passed directly to [Loading] state as it's prevented by a private constructor.
     * The only time it's used is when the existing data needs to be transformed. See
     * [ListNetworkResource.getTransformedListNetworkResource] for more details.
     *
     * @param loadingMore flag is used to indicate whether the first page or more data is being fetched. It's default
     * value is `false` which should be useful in situations where pagination is not available.
     *
     * @see loading helper function for easier initialization.
     */
    class Loading<T> private constructor(previous: ListNetworkResource<T>, data: List<T>, val loadingMore: Boolean)
        : ListNetworkResource<T>(previous, data) {
        constructor(previous: ListNetworkResource<T>, loadingMore: Boolean = false)
                : this(previous, previous.data, loadingMore)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Loading(this, transform(data), loadingMore)
    }

    /** This state means that at least one fetch has successfully completed.
     *
     * @param previous The previous state. The [Loading] state is expected to be passed in for it, but it's not forced.
     * See the property explanation in [ListNetworkResource] for more details.
     *
     * @param data This is the second and final state where the data can be passed in directly.
     *
     * @param canLoadMore For resources where pagination is available, this flag can be used to indicate if more data
     * can be fetched. It's default value is `false` which should be useful in situations where pagination is not
     * available.
     *
     * @see success helper function for easier initialization.
     */
    class Success<T>(previous: ListNetworkResource<T>, data: List<T>, val canLoadMore: Boolean = false)
        : ListNetworkResource<T>(previous, data) {
        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Success(this, transform(data), canLoadMore)
    }

    /**
     * This state means that at least one fetch has resulted in error.
     *
     * @param previous The previous state. The [Loading] state is expected to be passed in for it, but it's not forced.
     * [previous] property in this state can be used to check whether first page or more data was being loaded. See
     * the property explanation in [ListNetworkResource] for more details.
     *
     * @param data can not be passed directly to [Error] state as it's prevented by a private constructor.
     * The only time it's used is when the existing data needs to be transformed. See
     * [ListNetworkResource.getTransformedListNetworkResource] for more details.
     *
     * @param errorMessage will be the error string received from the API. It can also be used to show connection errors
     * where the network is not available.
     *
     * @see error helper function for easier initialization.
     *
     * Some possible improvements to this state would be to add a helper function to get a flag for whether
     * first page or more data was being loaded. Adding an error type `enum` could also prove useful.
     */
    class Error<T> private constructor(previous: ListNetworkResource<T>, data: List<T>, val errorMessage: String?)
        : ListNetworkResource<T>(previous, data) {
        constructor(previous: ListNetworkResource<T>, errorMessage: String?)
                : this(previous, previous.data, errorMessage)

        override fun getTransformedListNetworkResource(transform: (List<T>) -> List<T>) =
                Error(this, transform(data), errorMessage)
    }
}
