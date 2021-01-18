package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class ScanHistoryViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ScanHistoryViewModel

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryViewModel()
    }

    @Test
    fun `foo`() {
        Assertions.assertThat(true).isTrue
    }
}
