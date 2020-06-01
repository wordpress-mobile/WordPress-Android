package org.wordpress.android.fluxc.list

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.paging.PagedList
import com.nhaarman.mockitokotlin2.firstValue
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.TEST_SCOPE
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListDescriptorTypeIdentifier
import org.wordpress.android.fluxc.model.list.ListState
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.store.ListStore.ListError
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.ListStore.ListErrorType.PERMISSION_ERROR
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange
import org.wordpress.android.fluxc.store.ListStore.OnListChanged.CauseOfListChange.FIRST_PAGE_FETCHED
import org.wordpress.android.fluxc.store.ListStore.OnListDataInvalidated
import org.wordpress.android.fluxc.store.ListStore.OnListRequiresRefresh
import org.wordpress.android.fluxc.store.ListStore.OnListStateChanged

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PagedListWrapperTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockDispatcher = mock<Dispatcher>()
    private val mockListDescriptor = mock<ListDescriptor>()
    private val mockRefresh = mock<() -> Unit>()
    private val mockInvalidate = mock<() -> Unit>()

    private fun createPagedListWrapper(lifecycle: Lifecycle = mock()) = PagedListWrapper(
            data = MutableLiveData<PagedList<String>>(),
            dispatcher = mockDispatcher,
            listDescriptor = mockListDescriptor,
            lifecycle = lifecycle,
            refresh = mockRefresh,
            invalidate = mockInvalidate,
            parentCoroutineContext = TEST_SCOPE.coroutineContext
    )

    @Test
    fun `registers dispatcher and observes lifecycle in init`() {
        val mockLifecycle = mock<Lifecycle>()

        val pagedListWrapper = createPagedListWrapper(mockLifecycle)

        verify(mockDispatcher).register(pagedListWrapper)
        verify(mockLifecycle).addObserver(pagedListWrapper)
    }

    @Test
    fun `unregisters dispatcher and stops observing lifecycle on destroy`() {
        val lifecycle = LifecycleRegistry(mock())
        assertThat(lifecycle.observerCount).isEqualTo(0)
        lifecycle.markState(Lifecycle.State.CREATED)

        val pagedListWrapper = createPagedListWrapper(lifecycle)
        assertThat(lifecycle.observerCount).isEqualTo(1)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(mockDispatcher).register(pagedListWrapper)
        verify(mockDispatcher).unregister(pagedListWrapper)
        assertThat(lifecycle.observerCount).isEqualTo(0)
    }

    @Test
    fun `fetchFirstPage invokes refresh property`() {
        val pagedListWrapper = createPagedListWrapper()

        pagedListWrapper.fetchFirstPage()

        verify(mockRefresh).invoke()
    }

    @Test
    fun `invalidateData invokes invalidate property`() {
        val pagedListWrapper = createPagedListWrapper()

        pagedListWrapper.invalidateData()

        verify(mockInvalidate).invoke()
    }

    @Test
    fun `fetchingFirstPage ListState is propagated correctly`() {
        testListStateIsPropagatedCorrectly(ListState.FETCHING_FIRST_PAGE)
    }

    @Test
    fun `loadingMore ListState is propagated correctly`() {
        testListStateIsPropagatedCorrectly(ListState.LOADING_MORE)
    }

    @Test
    fun `default ListState is propagated correctly`() {
        testListStateIsPropagatedCorrectly(ListState.defaultState)
    }

    @Test
    fun `permission ListError is propagated correctly`() {
        testListStateIsPropagatedCorrectly(ListState.ERROR, ListError(PERMISSION_ERROR))
    }

    @Test
    fun `generic ListError is propagated correctly`() {
        testListStateIsPropagatedCorrectly(ListState.ERROR, ListError(GENERIC_ERROR))
    }

    @Test
    fun `onListChanged invokes invalidate property`() {
        triggerOnListChanged()
        verify(mockInvalidate).invoke()
    }

    @Test
    fun `onListRequiresRefresh invokes refresh`() {
        triggerOnListRequiresRefresh()
        verify(mockRefresh).invoke()
    }

    @Test
    fun `onListDataInvalidated invokes invalidate property`() {
        triggerOnListDataInvalidated()
        verify(mockInvalidate).invoke()
    }

    private fun testListStateIsPropagatedCorrectly(listState: ListState, listError: ListError? = null) {
        val pagedListWrapper = createPagedListWrapper()
        val isFetchingFirstPageObserver = mock<Observer<Boolean>>()
        val isLoadingMoreObserver = mock<Observer<Boolean>>()
        val listErrorObserver = mock<Observer<ListError?>>()
        pagedListWrapper.isFetchingFirstPage.observeForever(isFetchingFirstPageObserver)
        pagedListWrapper.isLoadingMore.observeForever(isLoadingMoreObserver)
        pagedListWrapper.listError.observeForever(listErrorObserver)

        val event = OnListStateChanged(mockListDescriptor, listState, listError)
        pagedListWrapper.onListStateChanged(event)

        captureAndVerifySingleValue(isFetchingFirstPageObserver, listState.isFetchingFirstPage())
        captureAndVerifySingleValue(isLoadingMoreObserver, listState.isLoadingMore())
        captureAndVerifySingleValue(listErrorObserver, listError)
    }

    private inline fun <reified T> captureAndVerifySingleValue(observer: Observer<T>, result: T) {
        val captor = ArgumentCaptor.forClass(T::class.java)
        verify(observer).onChanged(captor.capture())
        assertThat(captor.firstValue).isEqualTo(result)
    }

    private fun triggerOnListChanged(
        causeOfListChange: CauseOfListChange = FIRST_PAGE_FETCHED,
        error: ListError? = null
    ) {
        val pagedListWrapper = createPagedListWrapper()
        val event = OnListChanged(
                listDescriptors = listOf(mockListDescriptor),
                causeOfChange = causeOfListChange,
                error = error
        )
        pagedListWrapper.onListChanged(event)
    }

    private fun triggerOnListRequiresRefresh() {
        val pagedListWrapper = createPagedListWrapper()
        whenever(mockListDescriptor.typeIdentifier).thenReturn(ListDescriptorTypeIdentifier(0))
        val event = OnListRequiresRefresh(type = mockListDescriptor.typeIdentifier)
        pagedListWrapper.onListRequiresRefresh(event)
    }

    private fun triggerOnListDataInvalidated() {
        val pagedListWrapper = createPagedListWrapper()
        whenever(mockListDescriptor.typeIdentifier).thenReturn(ListDescriptorTypeIdentifier(0))
        val event = OnListDataInvalidated(type = mockListDescriptor.typeIdentifier)
        pagedListWrapper.onListDataInvalidated(event)
    }
}
