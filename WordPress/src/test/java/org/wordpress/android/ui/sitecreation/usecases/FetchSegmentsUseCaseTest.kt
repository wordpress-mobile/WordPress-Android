package org.wordpress.android.ui.sitecreation.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.VerticalStore
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FetchSegmentsUseCaseTest : BaseUnitTest() {
    @Mock
    lateinit var dispatcher: Dispatcher
    @Mock
    lateinit var store: VerticalStore
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
