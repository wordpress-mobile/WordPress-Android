package org.wordpress.android.ui.jetpack.restore

import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.complete.RestoreCompleteStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.complete.RestoreCompleteViewModel
import org.wordpress.android.ui.jetpack.restore.complete.RestoreCompleteViewModel.UiState
import java.util.Date

@InternalCoroutinesApi
class RestoreCompleteViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: RestoreCompleteViewModel
    @Mock private lateinit var parentViewModel: RestoreViewModel
    @Mock private lateinit var site: SiteModel
    private lateinit var stateListItemBuilder: RestoreCompleteStateListItemBuilder

    private val restoreState = RestoreState(
            activityId = "activityId",
            rewindId = "rewindId",
            restoreId = 100L,
            siteId = 200L,
            published = Date(1609690147756)
    )

    @Before
    fun setUp() = test {
        stateListItemBuilder = RestoreCompleteStateListItemBuilder()
        viewModel = RestoreCompleteViewModel(
                stateListItemBuilder,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `when started, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, restoreState, parentViewModel)

        assertThat(uiStates[0]).isInstanceOf(UiState::class.java)
    }

    @Test
    fun `when visit site is clicked, then a navigationEvent is posted`() = test {
        val uiStates = initObservers().uiStates
        val navigationEvents = initObservers().navigationEvents

        viewModel.start(site, restoreState, parentViewModel)
        whenever(site.url).thenReturn("www.google.com")

        ((uiStates.last().items).last { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(navigationEvents.last()).isInstanceOf(VisitSite::class.java)
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever { uiStates.add(it) }

        val navigationEvents = mutableListOf<RestoreNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        return Observers(uiStates, navigationEvents)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val navigationEvents: List<RestoreNavigationEvents>
    )
}
