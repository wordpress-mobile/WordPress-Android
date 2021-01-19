package org.wordpress.android.ui.jetpack.scan

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.ScanningStateUpdatedInDb
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.Event

private const val ON_START_SCAN_BUTTON_CLICKED_PARAM_POSITION = 2
private const val ON_FIX_ALL_THREATS_BUTTON_CLICKED_PARAM_POSITION = 3
private const val ON_THREAT_ITEM_CLICKED_PARAM_POSITION = 4

@InternalCoroutinesApi
class ScanViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStateItemsBuilder: ScanStateListItemsBuilder
    @Mock private lateinit var fetchScanStateUseCase: FetchScanStateUseCase
    @Mock private lateinit var startScanUseCase: StartScanUseCase
    @Mock private lateinit var fixThreatsUseCase: FixThreatsUseCase

    private lateinit var viewModel: ScanViewModel

    private val fakeScanStateModel = ScanStateModel(state = ScanStateModel.State.IDLE, hasCloud = true)
    private val fakeUiStringText = UiStringText("")
    private val fakeThreatId = 1L

    @Before
    fun setUp() = test {
        viewModel = ScanViewModel(
            scanStateItemsBuilder,
            fetchScanStateUseCase,
            startScanUseCase,
            fixThreatsUseCase,
            TEST_DISPATCHER
        )
        whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Success(fakeScanStateModel)))
        whenever(scanStateItemsBuilder.buildScanStateListItems(any(), any(), any(), any(), any())).thenAnswer {
            createDummyScanStateListItems(
                it.getArgument(ON_START_SCAN_BUTTON_CLICKED_PARAM_POSITION),
                it.getArgument(ON_FIX_ALL_THREATS_BUTTON_CLICKED_PARAM_POSITION),
                it.getArgument(ON_THREAT_ITEM_CLICKED_PARAM_POSITION)
            )
        }
    }

    @Test
    fun `when vm starts, fetch scan state is triggered`() = test {
        viewModel.start(site)

        verify(fetchScanStateUseCase).fetchScanState(site)
    }

    @Test
    fun `when scan state is fetched successfully, then ui is updated with content`() = test {
        val uiStates = init().uiStates

        assertThat(uiStates.last()).isInstanceOf(Content::class.java)
    }

    @Test
    fun `when threat item is clicked, then app navigates to threat details`() = test {
        val observers = init()

        (observers.uiStates.last() as Content).items.filterIsInstance<ThreatItemState>().first().onClick.invoke()

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowThreatDetails::class.java)
    }

    @Test
    fun `when scan button is clicked, then start scan is triggered`() = test {
        val uiStates = init().uiStates

        (uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        verify(startScanUseCase).startScan(site)
    }

    @Test
    fun `when scan button is clicked, then content updated on scan optimistic start (scanning state updated in db)`() =
        test {
            whenever(startScanUseCase.startScan(any()))
                .thenReturn(flowOf(ScanningStateUpdatedInDb(fakeScanStateModel)))
            val uiStates = init().uiStates

            (uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

            assertThat(uiStates.filterIsInstance<Content>()).size().isEqualTo(2)
        }

    @Test
    fun `when scan button is clicked, then fetch scan state is triggered on scan start success`() = test {
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Success))
        val uiStates = init().uiStates

        (uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        verify(fetchScanStateUseCase, times(2)).fetchScanState(site)
    }

    @Test
    fun `when fix all threats button is clicked, then fix threats confirmation dialog action is triggered`() =
        test {
            val observers = init()

            (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().last().onClick.invoke()

            val fixThreatsDialogAction = observers.navigation.last().peekContent()
            assertThat(fixThreatsDialogAction).isInstanceOf(OpenFixThreatsConfirmationDialog::class.java)
        }

    @Test
    fun `when fix threats confirmation dialog action is triggered, then fix threats confirmation dialog is shown`() =
        test {
            val observers = init()

            (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().last().onClick.invoke()

            val confirmationDialog = observers.navigation.last().peekContent() as OpenFixThreatsConfirmationDialog
            with(confirmationDialog) {
                assertThat(title).isEqualTo(UiStringRes(R.string.threat_fix_all_warning_title))
                assertThat(message).isEqualTo(
                    UiStringResWithParams(
                        R.string.threat_fix_all_warning_message,
                        listOf(UiStringText("1"))
                    )
                )
                assertThat(positiveButtonLabel).isEqualTo(R.string.dialog_button_ok)
                assertThat(negativeButtonLabel).isEqualTo(R.string.dialog_button_cancel)
            }
        }

    @Test
    fun `given no network, when fix threats is triggered, then network error message is shown`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any()))
            .thenReturn(FixThreatsState.Failure.NetworkUnavailable)
        val observers = init()

        triggerFixThreatsAction(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `when request to fix threats succeeds, then fix started message is shown`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Success)
        val observers = init()

        triggerFixThreatsAction(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_started_message)))
    }

    @Test
    fun `when request to fix threats fails, then fix threats error message is shown`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Failure.RemoteRequestFailure)
        val observers = init()

        triggerFixThreatsAction(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_error_message)))
    }

    @Test
    fun `when ok button on fix threats action confirmation dialog is clicked, then action buttons are disabled`() =
        test {
            val observers = init()

            triggerFixThreatsAction(observers)

            val contentItems = (observers.uiStates.last() as Content).items
            val disabledActionButtons = contentItems.filterIsInstance<ActionButtonState>().map { !it.isEnabled }
            assertThat(disabledActionButtons.size).isEqualTo(2)
        }

    @Test
    fun `when request to fix threats fails, then action buttons are enabled`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Failure.RemoteRequestFailure)
        val observers = init()

        triggerFixThreatsAction(observers)

        val contentItems = (observers.uiStates.last() as Content).items
        val enabledActionButtons = contentItems.filterIsInstance<ActionButtonState>().map { it.isEnabled }
        assertThat(enabledActionButtons.size).isEqualTo(2)
    }

    private fun triggerFixThreatsAction(observers: Observers) {
        (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().last().onClick.invoke()
        (observers.navigation.last().peekContent() as OpenFixThreatsConfirmationDialog).okButtonAction.invoke()
    }

    private fun createDummyScanStateListItems(
        onStartScanButtonClicked: (() -> Unit),
        onFixAllButtonClicked: (() -> Unit),
        onThreatItemClicked: (Long) -> Unit
    ) = listOf(
        ActionButtonState(
            text = fakeUiStringText,
            contentDescription = fakeUiStringText,
            isSecondary = false,
            onClick = onStartScanButtonClicked
        ),
        ActionButtonState(
            text = fakeUiStringText,
            contentDescription = fakeUiStringText,
            isSecondary = true,
            onClick = onFixAllButtonClicked
        ),
        ThreatItemState(
            threatId = fakeThreatId,
            isFixable = true,
            header = fakeUiStringText,
            subHeader = fakeUiStringText
        ) { onThreatItemClicked(fakeThreatId) }
    )

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val snackbarMsgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever {
            snackbarMsgs.add(it)
        }
        val navigation = mutableListOf<Event<ScanNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigation.add(it)
        }

        viewModel.start(site)

        return Observers(uiStates, snackbarMsgs, navigation)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val snackBarMsgs: List<Event<SnackbarMessageHolder>>,
        val navigation: List<Event<ScanNavigationEvents>>
    )
}
