package org.wordpress.android.ui.jetpack.scan.details

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.usecases.GetThreatModelUseCase
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@InternalCoroutinesApi
class ThreatDetailsViewModelTest : BaseUnitTest() {
    @Mock private lateinit var getThreatModelUseCase: GetThreatModelUseCase
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var builder: ThreatDetailsListItemsBuilder
    private lateinit var viewModel: ThreatDetailsViewModel
    private val threatId = 1L

    @Before
    fun setUp() {
        viewModel = ThreatDetailsViewModel(getThreatModelUseCase, selectedSiteRepository, builder)
    }

    @Test
    fun `given threat id, when on start, then threat details are retrieved`() = test {
        // Act
        viewModel.start(threatId)
        // Assert
        verify(getThreatModelUseCase).get(threatId)
    }

    @Test
    fun `given threat id, when on start, then ui is updated with content`() = test {
        // Arrange
        val uiStates = init().uiStates
        // Act
        viewModel.start(threatId)
        // Assert
        val uiState = uiStates.last()
        assertThat(uiState).isInstanceOf(Content::class.java)
    }

    private suspend fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        whenever(getThreatModelUseCase.get(anyLong())).thenReturn(mock())
        whenever(builder.buildThreatDetailsListItems(any(), any(), any(), any())).thenReturn(mock())
        return Observers(uiStates)
    }

    private data class Observers(val uiStates: List<UiState>)
}
