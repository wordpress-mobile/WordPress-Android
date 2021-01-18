package org.wordpress.android.ui.jetpack.scan.history

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.test

@RunWith(MockitoJUnitRunner::class)
class ScanHistoryListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ScanHistoryListViewModel

    @Before
    fun setUp() = test {
        viewModel = ScanHistoryListViewModel()
    }

    @Test
    fun `foo`() {
        assertThat(true).isTrue
    }
}
