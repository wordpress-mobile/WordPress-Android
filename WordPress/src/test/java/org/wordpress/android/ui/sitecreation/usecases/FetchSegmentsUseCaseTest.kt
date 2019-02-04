package org.wordpress.android.ui.sitecreation.usecases

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class FetchSegmentsUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var store: VerticalStore
    private lateinit var useCase: FetchSegmentsUseCase
    private val event = OnSegmentsFetched(emptyList(), null)

    @Before
    fun setUp() {
        useCase = FetchSegmentsUseCase(dispatcher, store)
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onSiteCategoriesFetched(event) }
        val resultEvent = useCase.fetchCategories()

        assertThat(resultEvent).isEqualTo(event)
    }
}
