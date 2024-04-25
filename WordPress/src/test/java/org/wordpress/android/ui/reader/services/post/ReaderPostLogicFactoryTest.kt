package org.wordpress.android.ui.reader.services.post

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.services.ServiceCompletionListener

@RunWith(MockitoJUnitRunner::class)
class ReaderPostLogicFactoryTest {
    @Mock
    lateinit var readerPostRepository: ReaderPostRepository

    private lateinit var factory: ReaderPostLogicFactory

    @Before
    fun setUp() {
        factory = ReaderPostLogicFactory(readerPostRepository)
    }

    @Test
    fun `create should return a PostLogic instance`() {
        val listener = ServiceCompletionListener {
            // no-op
        }
        val logic = factory.create(listener)
        assertThat(logic).isInstanceOf(ReaderPostLogic::class.java)
    }
}
