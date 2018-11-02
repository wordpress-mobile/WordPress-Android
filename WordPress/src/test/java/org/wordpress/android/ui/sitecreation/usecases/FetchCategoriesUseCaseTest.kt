package org.wordpress.android.ui.sitecreation.usecases

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentsFetched
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class FetchCategoriesUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    private lateinit var useCase: FetchCategoriesUseCase
    private val event = OnSegmentsFetched(emptyList(), null)

    @Before
    fun setUp() {
        useCase = FetchCategoriesUseCase(dispatcher)
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onSiteCategoriesFetched(event) } // dispatch the real result event
        val resultEvent = useCase.fetchCategories()

        assertThat(resultEvent).isEqualTo(event)
    }
}
