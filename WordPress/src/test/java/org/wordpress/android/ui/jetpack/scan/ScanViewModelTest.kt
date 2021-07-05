package org.wordpress.android.ui.jetpack.scan

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.scan.ScanStateModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ProgressState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ThreatItemState
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.OpenFixThreatsConfirmationDialog
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowContactSupport
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.scan.ScanNavigationEvents.ShowThreatDetails
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ContentUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.jetpack.scan.ScanViewModel.UiState.FullScreenLoadingUiState
import org.wordpress.android.ui.jetpack.scan.builders.ScanStateListItemsBuilder
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchFixThreatsStatusUseCase.FetchFixThreatsState
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Failure
import org.wordpress.android.ui.jetpack.scan.usecases.FetchScanStateUseCase.FetchScanState.Success
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState
import org.wordpress.android.ui.jetpack.scan.usecases.StartScanUseCase.StartScanState.ScanningStateUpdatedInDb
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.viewmodel.Event

private const val ON_START_SCAN_BUTTON_CLICKED_PARAM_POSITION = 3
private const val ON_FIX_ALL_THREATS_BUTTON_CLICKED_PARAM_POSITION = 4
private const val ON_THREAT_ITEM_CLICKED_PARAM_POSITION = 5
private const val ON_ENTER_SERVER_CREDS_MESSAGE_CLICKED_PARAM_POSITION = 7
private const val TEST_SITE_ID = 1L
private const val SERVER_CREDS_LINK = "${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID}"

@ExperimentalCoroutinesApi
@InternalCoroutinesApi
class ScanViewModelTest : BaseUnitTest() {
    @Rule
    @JvmField val coroutineScope = MainCoroutineScopeRule()

    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var scanStateItemsBuilder: ScanStateListItemsBuilder
    @Mock private lateinit var fetchScanStateUseCase: FetchScanStateUseCase
    @Mock private lateinit var startScanUseCase: StartScanUseCase
    @Mock private lateinit var fixThreatsUseCase: FixThreatsUseCase
    @Mock private lateinit var fetchFixThreatsStatusUseCase: FetchFixThreatsStatusUseCase
    @Mock private lateinit var scanStore: ScanStore
    @Mock private lateinit var scanTracker: ScanTracker
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils

    private lateinit var viewModel: ScanViewModel

    private val fakeScanStateModel = ScanStateModel(state = ScanStateModel.State.IDLE, hasCloud = true)
    private val fakeUiStringText = UiStringText("")
    private val fakeDetectedAt = UiStringText("")
    private val fakeSubHeaderColor = 1
    private val fakeThreatId = 1L
    private val fakeIconId = 1
    private val fakeIconBackgroundId = 1

    @Before
    fun setUp() = test {
        viewModel = ScanViewModel(
                scanStateItemsBuilder,
                fetchScanStateUseCase,
                startScanUseCase,
                fixThreatsUseCase,
                fetchFixThreatsStatusUseCase,
                scanStore,
                scanTracker,
                htmlMessageUtils,
                TEST_DISPATCHER
        )
        whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Success(fakeScanStateModel)))
        whenever(scanStateItemsBuilder.buildScanStateListItems(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer {
                    createDummyScanStateListItems(
                            it.getArgument(ON_START_SCAN_BUTTON_CLICKED_PARAM_POSITION),
                            it.getArgument(ON_FIX_ALL_THREATS_BUTTON_CLICKED_PARAM_POSITION),
                            it.getArgument(ON_THREAT_ITEM_CLICKED_PARAM_POSITION),
                            it.getArgument(ON_ENTER_SERVER_CREDS_MESSAGE_CLICKED_PARAM_POSITION)
                    )
                }
        whenever(scanStore.getScanStateForSite(site)).thenReturn(fakeScanStateModel)
        whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                flowOf(FetchFixThreatsState.Complete(fixedThreatsCount = 1))
        )
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt())).thenReturn("")
    }

    @Test
    fun `given last scan state not present in db, when vm starts, then app displays full screen loading scan state`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                val uiStates = init().uiStates

                assertThat(uiStates.first()).isInstanceOf(FullScreenLoadingUiState::class.java)
            }

    @Test
    fun `given last scan state present in db, when vm starts, then app displays full screen loading scan state`() =
            test {
                val uiStates = init().uiStates

                assertThat(uiStates.first()).isInstanceOf(FullScreenLoadingUiState::class.java)
            }

    @Test
    fun `when vm starts, fetch scan state is triggered`() = test {
        viewModel.start(site)

        verify(fetchScanStateUseCase).fetchScanState(site)
    }

    @Test
    fun `given fixable threats present in db, when vm starts, fetch fix threats status is triggered`() =
            test {
                val scanStateModelWithFixableThreats = fakeScanStateModel
                        .copy(threats = listOf(ThreatTestData.fixableThreatInCurrentStatus))
                whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModelWithFixableThreats)

                viewModel.start(site)

                verify(fetchFixThreatsStatusUseCase).fetchFixThreatsStatus(any(), any())
            }

    @Test
    fun `given no network, when scan state fetched over empty scan state, then app reaches no connection state`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.NetworkUnavailable))
                val uiStates = init().uiStates

                assertThat(uiStates.last()).isInstanceOf(ErrorUiState.NoConnection::class.java)
            }

    @Test
    fun `given no connection state, when scan state fetched over empty scan state, then no network ui is shown`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.NetworkUnavailable))
                val uiStates = init().uiStates

                val error = uiStates.last() as ErrorUiState
                with(error) {
                    assertThat(image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
                    assertThat(title).isEqualTo(UiStringRes(R.string.scan_no_network_title))
                    assertThat(subtitle).isEqualTo(UiStringRes(R.string.scan_no_network_subtitle))
                    assertThat(buttonText).isEqualTo(UiStringRes(R.string.retry))
                }
            }

    @Test
    fun `given no network, when scan state fetched over last scan state, then no network msg is shown`() = test {
        whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.NetworkUnavailable))
        val observers = init()

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `given fetch scan fails, when scan state fetched over empty scan state, then app reaches failed ui state`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.RemoteRequestFailure))
                val uiStates = init().uiStates

                assertThat(uiStates.last()).isInstanceOf(ErrorUiState.GenericRequestFailed::class.java)
            }

    @Test
    fun `given request failed ui state, when scan state fetched over empty scan state, then request failed ui shown`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.RemoteRequestFailure))
                val uiStates = init().uiStates

                val state = uiStates.last() as ErrorUiState
                with(state) {
                    assertThat(image).isEqualTo(R.drawable.img_illustration_cloud_off_152dp)
                    assertThat(title).isEqualTo(UiStringRes(R.string.scan_request_failed_title))
                    assertThat(subtitle).isEqualTo(UiStringRes(R.string.scan_request_failed_subtitle))
                    assertThat(buttonText).isEqualTo(UiStringRes(R.string.contact_support))
                }
            }

    @Test
    fun `given fetch scan state fails, when scan state fetched over last scan state, then request failed msg shown`() =
            test {
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.RemoteRequestFailure))
                val observers = init()

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.request_failed_message)))
            }

    @Test
    fun `given fetch scan state succeeds with valid state, when scan state is fetched, then ui updated with content`() =
            test {
                val uiStates = init().uiStates

                assertThat(uiStates.last()).isInstanceOf(ContentUiState::class.java)
            }

    @Test
    fun `given fetch scan state succeeds with invalid state, when scan state is fetched, then error ui is shown`() =
            test {
                whenever(fetchScanStateUseCase.fetchScanState(site))
                        .thenReturn(flowOf(Success(fakeScanStateModel.copy(state = ScanStateModel.State.UNKNOWN))))

                val uiStates = init().uiStates

                val errorState = uiStates.last() as ErrorUiState
                assertThat(errorState).isInstanceOf(ErrorUiState.ScanRequestFailed::class.java)
            }

    @Test
    fun `given invalid scan state error ui, when contact support is clicked, then contact support is shown`() = test {
        whenever(fetchScanStateUseCase.fetchScanState(site))
                .thenReturn(flowOf(Success(fakeScanStateModel.copy(state = ScanStateModel.State.UNKNOWN))))
        val observers = init()

        (observers.uiStates.last() as ErrorUiState).action.invoke()

        assertThat(observers.navigation.last().peekContent()).isEqualTo(ShowContactSupport(site))
    }

    @Test
    fun `given no network error ui state, when retry is clicked, then fetch scan state is triggered`() =
            coroutineScope.runBlockingTest {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.NetworkUnavailable))
                val uiStates = init().uiStates

                (uiStates.last() as ErrorUiState).action.invoke()
                advanceTimeBy(RETRY_DELAY)

                verify(fetchScanStateUseCase, times(2)).fetchScanState(site)
            }

    @Test
    fun `given request failed error ui state, when contact support is clicked, then contact support screen is shown`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Failure.RemoteRequestFailure))
                val observers = init()

                (observers.uiStates.last() as ErrorUiState).action.invoke()

                assertThat(observers.navigation.last().peekContent()).isEqualTo(ShowContactSupport(site))
            }

    @Test
    fun `when threat item is clicked, then app navigates to threat details`() = test {
        val observers = init()

        (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ThreatItemState>().first().onClick.invoke()

        assertThat(observers.navigation.last().peekContent()).isInstanceOf(ShowThreatDetails::class.java)
    }

    @Test
    fun `when scan button is clicked, then start scan is triggered`() = test {
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(ScanningStateUpdatedInDb(fakeScanStateModel)))
        val uiStates = init().uiStates

        (uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        verify(startScanUseCase).startScan(site)
    }

    @Test
    fun `when scan button is clicked, then content updated on scan optimistic start (scanning state updated in db)`() =
            test {
                whenever(scanStore.getScanStateForSite(site)).thenReturn(null)
                whenever(startScanUseCase.startScan(any()))
                        .thenReturn(flowOf(ScanningStateUpdatedInDb(fakeScanStateModel)))
                val uiStates = init().uiStates

                (uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

                assertThat(uiStates.filterIsInstance<ContentUiState>()).size().isEqualTo(2)
            }

    @Test
    fun `when server creds icon is clicked, then app opens site's jetpack settings external url`() = test {
        whenever(site.siteId).thenReturn(TEST_SITE_ID)
        val observers = init()

        (observers.uiStates.last() as ContentUiState).items
                .filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_LINK) }
                ?.onIconClick
                ?.invoke()

        assertThat(observers.navigation.last().peekContent()).isEqualTo(
                ShowJetpackSettings("${Constants.URL_JETPACK_SETTINGS}/${site.siteId}")
        )
    }

    @Test
    fun `given no network, when scan button is clicked, then no network msg is shown`() = test {
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Failure.NetworkUnavailable))
        val observers = init()

        (observers.uiStates.last() as ContentUiState)
                .items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `given scan start request fails, when scan button is clicked, then app reaches scan request failed state`() =
            test {
                whenever(startScanUseCase.startScan(any()))
                        .thenReturn(flowOf(StartScanState.Failure.RemoteRequestFailure))
                val observers = init()

                (observers.uiStates.last() as ContentUiState)
                        .items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

                assertThat(observers.uiStates.last()).isInstanceOf(ErrorUiState.ScanRequestFailed::class.java)
            }

    @Test
    fun `given scan request failed state, when scan button is clicked, then request failed ui is shown`() = test {
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Failure.RemoteRequestFailure))
        val observers = init()

        (observers.uiStates.last() as ContentUiState)
                .items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        val errorState = observers.uiStates.last() as ErrorUiState
        with(errorState) {
            assertThat(image).isEqualTo(R.drawable.img_illustration_empty_results_216dp)
            assertThat(title).isEqualTo(UiStringRes(R.string.scan_start_request_failed_title))
            assertThat(subtitle).isEqualTo(UiStringRes(R.string.scan_start_request_failed_subtitle))
            assertThat(buttonText).isEqualTo(UiStringRes(R.string.contact_support))
        }
    }

    @Test
    fun `given scan request failed error state, when contact support is clicked, then contact support shown`() =
            test {
                whenever(startScanUseCase.startScan(any()))
                        .thenReturn(flowOf(StartScanState.Failure.RemoteRequestFailure))
                val observers = init()

                (observers.uiStates.last() as ContentUiState)
                        .items.filterIsInstance<ActionButtonState>().first().onClick.invoke()
                (observers.uiStates.last() as ErrorUiState).action.invoke()

                assertThat(observers.navigation.last().peekContent()).isEqualTo(ShowContactSupport(site))
            }

    @Test
    fun `given scan start succeeds, when scan button is clicked, then scan state is fetched after delay`() = test {
        whenever(fetchScanStateUseCase.fetchScanState(site = site, startWithDelay = true))
                .thenReturn(flowOf(Success(fakeScanStateModel)))
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Success))
        val uiStates = init().uiStates

        (uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()

        verify(fetchScanStateUseCase).fetchScanState(site = site, startWithDelay = true)
    }

    @Test
    fun `given no threats found, when scan button is clicked, then no threats message is displayed`() = test {
        val noThreatsFoundMessage = "no threats found"
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(R.string.scan_finished_no_threats_found_message))
                .thenReturn(noThreatsFoundMessage)
        whenever(fetchScanStateUseCase.fetchScanState(site = site, startWithDelay = true))
                .thenReturn(flowOf(Success(fakeScanStateModel)))
        whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Success))
        val observers = init()

        (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first()
                .onClick.invoke()

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringText(noThreatsFoundMessage)))
    }

    @Test
    fun `given single threat found, when scan button is clicked, then single threat found message is displayed`() =
            test {
                val threatFoundMessage = "1 potential threat found"
                whenever(
                        htmlMessageUtils
                                .getHtmlMessageFromStringFormatResId(
                                        R.string.scan_finished_potential_threats_found_message_singular
                                )
                ).thenReturn(threatFoundMessage)
                val fakeScanStateModelWithThreat = fakeScanStateModel.copy(
                        threats = listOf(ThreatTestData.genericThreatModel)
                )
                whenever(fetchScanStateUseCase.fetchScanState(site = site, startWithDelay = true))
                        .thenReturn(flowOf(Success(fakeScanStateModelWithThreat)))
                whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Success))
                val observers = init()

                (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first()
                        .onClick.invoke()

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringText(threatFoundMessage)))
            }

    @Test
    fun `given multiple threats found, when scan button is clicked, then plural threats found message is displayed`() =
            test {
                val threats = listOf(ThreatTestData.genericThreatModel, ThreatTestData.genericThreatModel)
                val threatsFoundMessage = "${threats.size} potential threats found."
                whenever(
                        htmlMessageUtils
                                .getHtmlMessageFromStringFormatResId(
                                        R.string.scan_finished_potential_threats_found_message_plural,
                                        "${threats.size}"
                                )
                ).thenReturn(threatsFoundMessage)
                val fakeScanStateModelWithThreats = fakeScanStateModel.copy(threats = threats)
                whenever(fetchScanStateUseCase.fetchScanState(site = site, startWithDelay = true))
                        .thenReturn(flowOf(Success(fakeScanStateModelWithThreats)))
                whenever(startScanUseCase.startScan(any())).thenReturn(flowOf(StartScanState.Success))
                val observers = init()

                (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>().first()
                        .onClick.invoke()

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringText(threatsFoundMessage)))
            }

    @Test
    fun `when fix all threats button is clicked, then fix threats confirmation dialog action is triggered`() =
            test {
                val observers = init()

                (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>()
                        .last().onClick.invoke()

                val fixThreatsDialogAction = observers.navigation.last().peekContent()
                assertThat(fixThreatsDialogAction).isInstanceOf(OpenFixThreatsConfirmationDialog::class.java)
            }

    @Test
    fun `when fix threat confirmation dialog action is triggered, then fix threat confirmation dialog is shown`() =
            test {
                val scanStateModelWithFixableThreats = fakeScanStateModel
                        .copy(threats = listOf(ThreatTestData.fixableThreatInCurrentStatus))
                whenever(fetchScanStateUseCase.fetchScanState(site))
                        .thenReturn(flowOf(Success(scanStateModelWithFixableThreats)))
                val observers = init()

                (observers.uiStates.last() as ContentUiState).items.filterIsInstance<ActionButtonState>()
                        .last().onClick.invoke()

                val confirmationDialog = observers.navigation.last().peekContent() as OpenFixThreatsConfirmationDialog
                with(confirmationDialog) {
                    assertThat(title).isEqualTo(UiStringRes(R.string.threat_fix_all_warning_title))
                    assertThat(message).isEqualTo(
                            UiStringRes(R.string.threat_fix_all_confirmation_message_singular)
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
    fun `given invalid response, when fix threats action is triggered, then fix threats error message is shown`() =
            test {
                whenever(fixThreatsUseCase.fixThreats(any(), any()))
                        .thenReturn(FixThreatsState.Failure.RemoteRequestFailure)
                val observers = init()

                triggerFixThreatsAction(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg)
                        .isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_error_message)))
            }

    @Test
    fun `given threats are fixed, when threats fix status is checked, then pluralised success message is shown`() =
            test {
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(FetchFixThreatsState.Complete(fixedThreatsCount = 2))
                )
                val observers = init()

                fetchFixThreatsStatus(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(
                        SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_status_success_message_plural))
                )
            }

    @Test
    fun `given single threat is fixed, when threat fix status is checked, then single threat success msg is shown`() =
            test {
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(FetchFixThreatsState.Complete(fixedThreatsCount = 1))
                )
                val observers = init()

                fetchFixThreatsStatus(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(
                        SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_status_success_message_singular))
                )
            }

    @Test
    fun `given no network, when threats fix status is checked, then network error message is shown`() = test {
        whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                flowOf(FetchFixThreatsState.Failure.NetworkUnavailable)
        )
        val observers = init()

        fetchFixThreatsStatus(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `given server is unavailable, when threats fix status is checked, then error message is shown`() = test {
        whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                flowOf(FetchFixThreatsState.Failure.RemoteRequestFailure)
        )
        val observers = init()

        fetchFixThreatsStatus(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(
                SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_status_error_message))
        )
    }

    @Test
    fun `given threats fixing fails, when threats fix status checked, then fix threats error message shown`() =
            test {
                whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Success)
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(
                                FetchFixThreatsState.Failure.FixFailure(
                                        containsOnlyErrors = true,
                                        mightBeMissingCredentials = false
                                )
                        )
                )
                val observers = init()

                triggerFixThreatsAction(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(
                        SnackbarMessageHolder(UiStringRes(R.string.threat_fix_all_status_error_message))
                )
            }

    @Test
    fun `given some threats not fixed, when threats fix status checked, then scan state is re-fetched`() =
            test {
                whenever(fetchScanStateUseCase.fetchScanState(site)).thenReturn(flowOf(Success(mock())))
                whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Success)
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(
                                FetchFixThreatsState.Failure.FixFailure(
                                        containsOnlyErrors = false,
                                        mightBeMissingCredentials = false
                                )
                        )
                )
                val observers = init()

                triggerFixThreatsAction(observers)

                verify(fetchScanStateUseCase, times(2)).fetchScanState(site = site)
            }

    @Test
    fun `given activity result fix threat status data, when fix status is requested, then fix status is fetched`() =
            test {
                whenever(site.siteId).thenReturn(1L)
                viewModel.start(site)

                viewModel.onFixStateRequested(threatId = 11L)

                verify(fetchFixThreatsStatusUseCase).fetchFixThreatsStatus(
                        remoteSiteId = 1L,
                        fixableThreatIds = listOf(11L)
                )
            }

    @Test
    fun `given activity result request scan state data, when scan state is requested, then snackbar msg is shown`() =
            test {
                val observers = init()

                viewModel.onScanStateRequestedWithMessage(R.string.threat_ignore_success_message)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg)
                        .isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_ignore_success_message)))
            }

    @Test
    fun `given activity result request scan state data, when scan state is requested, then scan state is fetched`() =
            test {
                viewModel.start(site)

                viewModel.onScanStateRequestedWithMessage(R.string.threat_ignore_success_message)

                verify(fetchScanStateUseCase, times(2)).fetchScanState(site)
            }

    @Test
    fun `given FixFailure(onlyErr=true) returned, when fetch fix status invoked by user, then snackbar is shown`() =
            test {
                val messages = init().snackBarMsgs
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(
                                FetchFixThreatsState.Failure.FixFailure(
                                        containsOnlyErrors = true,
                                        mightBeMissingCredentials = false
                                )
                        )
                )

                viewModel.onFixStateRequested(threatId = 11L)

                assertThat(messages.isNotEmpty()).isTrue
            }

    @Test
    fun `given FixFailure(onlyErr=true) returned, when fetchStatus NOT invoked by user, then snackbar is NOT shown`() =
            test {
                whenever(fetchFixThreatsStatusUseCase.fetchFixThreatsStatus(any(), any())).thenReturn(
                        flowOf(
                                FetchFixThreatsState.Failure.FixFailure(
                                        containsOnlyErrors = true,
                                        mightBeMissingCredentials = false
                                )
                        )
                )
                val scanStateModelWithFixableThreats = fakeScanStateModel
                        .copy(threats = listOf(ThreatTestData.fixableThreatInCurrentStatus))
                whenever(scanStore.getScanStateForSite(site)).thenReturn(scanStateModelWithFixableThreats)

                val messages = init().snackBarMsgs

                assertThat(messages.isEmpty()).isTrue
            }

    private fun triggerFixThreatsAction(observers: Observers) {
        (observers.uiStates.last() as ContentUiState)
                .items.filterIsInstance<ActionButtonState>().last().onClick.invoke()
        (observers.navigation.last().peekContent() as OpenFixThreatsConfirmationDialog).okButtonAction.invoke()
    }

    private suspend fun fetchFixThreatsStatus(observers: Observers) {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Success)
        triggerFixThreatsAction(observers)
    }

    private fun createDummyScanStateListItems(
        onStartScanButtonClicked: (() -> Unit),
        onFixAllButtonClicked: (() -> Unit),
        onThreatItemClicked: (Long) -> Unit,
        onEnterServerCredsIconClicked: () -> Unit
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
            ProgressState(
                    progress = 0,
                    progressLabel = fakeUiStringText,
                    isIndeterminate = true,
                    isVisible = false
            ),
            FootnoteState(
                    iconResId = R.drawable.ic_plus_white_24dp,
                    iconColorResId = R.color.primary,
                    text = UiStringText(SERVER_CREDS_LINK),
                    onIconClick = onEnterServerCredsIconClicked
            ),
            ThreatItemState(
                    threatId = fakeThreatId,
                    isFixing = false,
                    firstDetectedDate = fakeDetectedAt,
                    header = fakeUiStringText,
                    subHeader = fakeUiStringText,
                    subHeaderColor = fakeSubHeaderColor,
                    icon = fakeIconId,
                    iconBackground = fakeIconBackgroundId
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
