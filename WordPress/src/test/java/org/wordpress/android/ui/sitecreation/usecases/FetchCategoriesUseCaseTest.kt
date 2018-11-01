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
import org.wordpress.android.test
import org.wordpress.android.ui.sitecreation.OnSiteCategoriesFetchedDummy

@RunWith(MockitoJUnitRunner::class)
class FetchCategoriesUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var dispatcher: Dispatcher
    private lateinit var useCase: FetchCategoriesUseCase
    private val dummyEvent = OnSiteCategoriesFetchedDummy()

    @Before
    fun setUp() {
        useCase = FetchCategoriesUseCase(dispatcher)
        // TODO add dispatcher.register and use .thanCallRealMethod
    }

    @Test
    fun coroutineResumedWhenResultEventDispatched() = test {
        whenever(dispatcher.dispatch(any())).then { useCase.onSiteCategoriesFetched(dummyEvent) } // dispatch the real result event
        val event = useCase.fetchCategories()

        assertThat(event).isEqualTo(dummyEvent)
    }
}
