package org.wordpress.android.fluxc.list

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.MutableLiveData
import android.arch.paging.PagedList
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.PagedListItemType
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import android.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import android.arch.lifecycle.LifecycleRegistry
import com.nhaarman.mockitokotlin2.times
import org.junit.Rule

private fun onlyOnce() = times(1)

class PagedListWrapperTest {
    @get:Rule
    val rule = InstantTaskExecutorRule()

    private val mockLiveData = MutableLiveData<PagedList<PagedListItemType<String>>>()
    private val mockDispatcher = mock<Dispatcher>()
    private val mockListDescriptor = mock<ListDescriptor>()
    private val mockRefresh = mock<() -> Unit>()
    private val mockInvalidate = mock<() -> Unit>()
    private val mockIsListEmpty = mock<() -> Boolean>()

    private fun createPagedListWrapper(lifecycle: Lifecycle = mock()) = PagedListWrapper(
            data = mockLiveData,
            dispatcher = mockDispatcher,
            listDescriptor = mockListDescriptor,
            lifecycle = lifecycle,
            refresh = mockRefresh,
            invalidate = mockInvalidate,
            isListEmpty = mockIsListEmpty
    )

    @Test
    fun `registers dispatcher and observes lifecycle in init`() {
        val mockLifecycle = mock<Lifecycle>()

        val pagedListWrapper = createPagedListWrapper(mockLifecycle)

        verify(mockDispatcher, onlyOnce()).register(pagedListWrapper)
        verify(mockLifecycle, onlyOnce()).addObserver(pagedListWrapper)
    }

    @Test
    fun `isListEmpty is updated in init`() {
        createPagedListWrapper()

        verify(mockIsListEmpty, onlyOnce()).invoke()
    }

    @Test
    fun `unregisters dispatcher and stops observing lifecycle on destroy`() {
        val lifecycle = LifecycleRegistry(mock())
        assertThat(lifecycle.observerCount).isEqualTo(0)
        lifecycle.markState(Lifecycle.State.CREATED)

        val pagedListWrapper = createPagedListWrapper(lifecycle)
        assertThat(lifecycle.observerCount).isEqualTo(1)
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        verify(mockDispatcher, onlyOnce()).register(pagedListWrapper)
        verify(mockDispatcher, onlyOnce()).unregister(pagedListWrapper)
        assertThat(lifecycle.observerCount).isEqualTo(0)
    }
}
