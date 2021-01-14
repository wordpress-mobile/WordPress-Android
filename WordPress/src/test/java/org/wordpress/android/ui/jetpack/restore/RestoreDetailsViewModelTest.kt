package org.wordpress.android.ui.jetpack.restore

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.restore.details.RestoreDetailsStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.details.RestoreDetailsViewModel
import org.wordpress.android.ui.jetpack.restore.details.RestoreDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import java.util.Date

@InternalCoroutinesApi
class RestoreDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: RestoreDetailsViewModel
    private lateinit var availableItemsProvider: JetpackAvailableItemsProvider
    private lateinit var stateListItemBuilder: RestoreDetailsStateListItemBuilder
    @Mock private lateinit var getActivityLogItemUseCase: GetActivityLogItemUseCase
    @Mock private lateinit var parentViewModel: RestoreViewModel
    @Mock private lateinit var site: SiteModel

    private val activityId = "1"

    @Before
    fun setUp() = test {
        availableItemsProvider = JetpackAvailableItemsProvider()
        stateListItemBuilder = RestoreDetailsStateListItemBuilder()
        viewModel = RestoreDetailsViewModel(
                availableItemsProvider,
                getActivityLogItemUseCase,
                stateListItemBuilder,
                TEST_DISPATCHER
        )
        whenever(getActivityLogItemUseCase.get(anyOrNull())).thenReturn(fakeActivityLogModel)
    }

    @Test
    fun `when available items are fetched, the content view is shown`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        Assertions.assertThat(uiStates[0]).isInstanceOf(UiState::class.java)
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        Assertions.assertThat((((uiStates.last()).items)
                .first { it is CheckboxState } as CheckboxState).checked).isFalse
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        viewModel.start(site, activityId, parentViewModel)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()
        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        Assertions.assertThat(((uiStates.last().items).first { it is CheckboxState } as CheckboxState).checked).isTrue
    }

    private fun initObservers(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        return Observers(uiStates)
    }

    private data class Observers(
        val uiStates: List<UiState>
    )

    private val fakeActivityLogModel: ActivityLogModel = ActivityLogModel(
            activityID = "1",
            summary = "summary",
            content = null,
            name = null,
            type = null,
            gridicon = null,
            status = null,
            rewindable = null,
            rewindID = "rewindId",
            published = Date()
    )
}
