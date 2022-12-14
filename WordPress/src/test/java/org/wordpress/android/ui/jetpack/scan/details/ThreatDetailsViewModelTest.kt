package org.wordpress.android.ui.jetpack.scan.details

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.ScanStore
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.DescriptionState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.scan.ThreatTestData
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.OpenThreatActionDialog
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.ShowUpdatedFixState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsNavigationEvents.ShowUpdatedScanStateWithMessage
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState
import org.wordpress.android.ui.jetpack.scan.details.ThreatDetailsViewModel.UiState.Content
import org.wordpress.android.ui.jetpack.scan.details.usecases.GetThreatModelUseCase
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase.IgnoreThreatState.Failure
import org.wordpress.android.ui.jetpack.scan.details.usecases.IgnoreThreatUseCase.IgnoreThreatState.Success
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase
import org.wordpress.android.ui.jetpack.scan.usecases.FixThreatsUseCase.FixThreatsState
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.analytics.ScanTracker
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider

private const val ON_FIX_THREAT_BUTTON_CLICKED_PARAM_POSITION = 3
private const val ON_GET_FREE_ESTIMATE_BUTTON_CLICKED_PARAM_POSITION = 4
private const val ON_IGNORE_THREAT_BUTTON_CLICKED_PARAM_POSITION = 5
private const val ON_ENTER_SERVER_CREDS_ICON_CLICKED_PARAM_POSITION = 6
private const val TEST_SITE_NAME = "test site name"
private const val TEST_SITE_ID = 1L
private const val SERVER_CREDS_LINK = "${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID}"

@ExperimentalCoroutinesApi
class ThreatDetailsViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var getThreatModelUseCase: GetThreatModelUseCase
    @Mock private lateinit var ignoreThreatUseCase: IgnoreThreatUseCase
    @Mock private lateinit var fixThreatsUseCase: FixThreatsUseCase
    @Mock private lateinit var selectedSiteRepository: SelectedSiteRepository
    @Mock private lateinit var builder: ThreatDetailsListItemsBuilder
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var resourceProvider: ResourceProvider
    @Mock private lateinit var scanTracker: ScanTracker
    @Mock private lateinit var scanStore: ScanStore

    private lateinit var viewModel: ThreatDetailsViewModel
    private val threatId = 1L
    private val fakeUiStringText = UiStringText("")
    private val fakeThreatModel = ThreatTestData.fixableThreatInCurrentStatus.copy(
            baseThreatModel = ThreatTestData.fixableThreatInCurrentStatus.baseThreatModel.copy(id = threatId)
    )

    @Before
    fun setUp() = test {
        viewModel = ThreatDetailsViewModel(
                getThreatModelUseCase,
                ignoreThreatUseCase,
                fixThreatsUseCase,
                selectedSiteRepository,
                scanStore,
                builder,
                htmlMessageUtils,
                resourceProvider,
                scanTracker
        )
        whenever(site.name).thenReturn(TEST_SITE_NAME)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(any(), any())).thenReturn(mock())
        whenever(getThreatModelUseCase.get(anyLong())).thenReturn(fakeThreatModel)
        whenever(builder.buildThreatDetailsListItems(any(), any(), any(), any(), any(), any(), any())).thenAnswer {
            createDummyThreatDetailsListItems(
                    it.getArgument(ON_FIX_THREAT_BUTTON_CLICKED_PARAM_POSITION),
                    it.getArgument(ON_IGNORE_THREAT_BUTTON_CLICKED_PARAM_POSITION),
                    it.getArgument(ON_ENTER_SERVER_CREDS_ICON_CLICKED_PARAM_POSITION)
            )
        }
        whenever(builder.buildFixableThreatDescription(any())).thenAnswer {
            DescriptionState(UiStringRes(R.string.threat_fix_fixable_edit))
        }
        whenever(scanStore.hasValidCredentials(site)).thenReturn(true)
    }

    @Test
    fun `given threat id, when on start, then threat details are retrieved`() = test {
        viewModel.start(threatId)

        verify(getThreatModelUseCase).get(threatId)
    }

    @Test
    fun `given threat id, when on start, then ui is updated with content`() = test {
        val uiStates = init().uiStates

        val uiState = uiStates.last()
        assertThat(uiState).isInstanceOf(Content::class.java)
    }

    @Test
    fun `when fix threat button is clicked, then open threat action dialog action is triggered for fix threat`() =
            test {
                val observers = init()

                (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                        .first().onClick.invoke()

                assertThat(observers.navigation.last().peekContent()).isInstanceOf(OpenThreatActionDialog::class.java)
            }

    @Test
    fun `when open threat action dialog is triggered for fix threat, then fix threat confirmation dialog is shown`() =
            test {
                val observers = init()

                (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                        .first().onClick.invoke()

                val confirmationDialog = observers.navigation.last().peekContent() as OpenThreatActionDialog
                with(confirmationDialog) {
                    assertThat(title).isEqualTo(UiStringRes(R.string.threat_fix))
                    val fixable = requireNotNull(fakeThreatModel.baseThreatModel.fixable)
                    assertThat(message).isEqualTo(builder.buildFixableThreatDescription(fixable).text)
                    assertThat(positiveButtonLabel).isEqualTo(R.string.dialog_button_ok)
                    assertThat(negativeButtonLabel).isEqualTo(R.string.dialog_button_cancel)
                }
            }

    @Test
    fun `when get free estimate button is clicked, then ShowGetFreeEstimate event is triggered`() = test {
        whenever(builder.buildThreatDetailsListItems(any(), any(), any(), any(), any(), any(), any())).thenAnswer {
            createDummyThreatDetailsListItems(
                    it.getArgument(ON_GET_FREE_ESTIMATE_BUTTON_CLICKED_PARAM_POSITION)
            )
        }
        val observers = init()

        (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                .first().onClick.invoke()

        assertThat(observers.navigation.last().peekContent())
                .isInstanceOf(ThreatDetailsNavigationEvents.ShowGetFreeEstimate::class.java)
    }

    @Test
    fun `when enter server creds icon is clicked, then app opens site's jetpack settings external url`() = test {
        whenever(builder.buildThreatDetailsListItems(any(), any(), any(), any(), any(), any(), any())).thenAnswer {
            createDummyThreatDetailsListItems(
                    onEnterServerCredsIconClicked = it
                            .getArgument(ON_ENTER_SERVER_CREDS_ICON_CLICKED_PARAM_POSITION)
            )
        }
        val observers = init()

        (observers.uiStates.last() as Content)
                .items
                .filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_LINK) }
                ?.onIconClick
                ?.invoke()

        assertThat(observers.navigation.last().peekContent()).isEqualTo(
                ShowJetpackSettings("${Constants.URL_JETPACK_SETTINGS}/${site.siteId}")
        )
    }

    @Test
    fun `given server unavailable, when fix threat action is triggered, then fix threat error msg is shown`() =
            test {
                whenever(fixThreatsUseCase.fixThreats(any(), any()))
                        .thenReturn(FixThreatsState.Failure.RemoteRequestFailure)
                val observers = init()

                triggerFixThreatAction(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_fix_error_message)))
            }

    @Test
    fun `given no network, when fix threat action is triggered, then network error msg is shown`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Failure.NetworkUnavailable)
        val observers = init()

        triggerFixThreatAction(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `when ok button on fix action confirmation dialog is clicked, then action buttons are disabled`() = test {
        val observers = init()

        triggerFixThreatAction(observers)

        val contentItems = (observers.uiStates.last() as Content).items
        val ignoreThreatButton = contentItems.filterIsInstance<ActionButtonState>().first()
        assertThat(ignoreThreatButton.isEnabled).isEqualTo(false)
    }

    @Test
    fun `given failure response, when fix threat action is triggered, then action buttons are enabled`() = test {
        whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Failure.RemoteRequestFailure)
        val observers = init()

        triggerFixThreatAction(observers)

        val contentItems = (observers.uiStates.last() as Content).items
        val ignoreThreatButton = contentItems.filterIsInstance<ActionButtonState>().first()
        assertThat(ignoreThreatButton.isEnabled).isEqualTo(true)
    }

    @Test
    fun `given success response, when fix threat action is triggered, then update fix state action is triggered`() =
            test {
                whenever(fixThreatsUseCase.fixThreats(any(), any())).thenReturn(FixThreatsState.Success)
                val observers = init()

                triggerFixThreatAction(observers)

                assertThat(observers.navigation.last().peekContent()).isEqualTo(ShowUpdatedFixState(threatId))
            }

    @Test
    fun `when ignore threat button is clicked, then open threat action dialog action is triggered for ignore threat`() =
            test {
                val observers = init()

                (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                        .last().onClick.invoke()

                assertThat(observers.navigation.last().peekContent()).isInstanceOf(OpenThreatActionDialog::class.java)
            }

    @Test
    fun `when open threat action dialog triggered for ignore threat, then ignore threat confirmation dialog shown`() =
            test {
                val observers = init()

                (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>()
                        .last().onClick.invoke()

                val siteName = site.name ?: resourceProvider.getString(R.string.scan_this_site)
                val confirmationDialog = observers.navigation.last().peekContent() as OpenThreatActionDialog
                with(confirmationDialog) {
                    assertThat(title).isEqualTo(UiStringRes(R.string.threat_ignore))
                    assertThat(message).isEqualTo(
                            UiStringText(
                                    htmlMessageUtils
                                            .getHtmlMessageFromStringFormatResId(
                                                    R.string.threat_ignore_warning,
                                                    "<b>$siteName</b>"
                                            )
                            )
                    )
                    assertThat(positiveButtonLabel).isEqualTo(R.string.dialog_button_ok)
                    assertThat(negativeButtonLabel).isEqualTo(R.string.dialog_button_cancel)
                }
            }

    @Test
    fun `given server unavailable, when ignore threat action is triggered, then ignore threat error msg is shown`() =
            test {
                whenever(ignoreThreatUseCase.ignoreThreat(any(), any())).thenReturn(Failure.RemoteRequestFailure)
                val observers = init()

                triggerIgnoreThreatAction(observers)

                val snackBarMsg = observers.snackBarMsgs.last().peekContent()
                assertThat(snackBarMsg)
                        .isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.threat_ignore_error_message)))
            }

    @Test
    fun `given no network, when ignore threat action is triggered, then network error msg is shown`() = test {
        whenever(ignoreThreatUseCase.ignoreThreat(any(), any())).thenReturn(Failure.NetworkUnavailable)
        val observers = init()

        triggerIgnoreThreatAction(observers)

        val snackBarMsg = observers.snackBarMsgs.last().peekContent()
        assertThat(snackBarMsg).isEqualTo(SnackbarMessageHolder(UiStringRes(R.string.error_generic_network)))
    }

    @Test
    fun `when ok button on ignore action confirmation dialog is clicked, then action buttons are disabled`() = test {
        val observers = init()

        triggerIgnoreThreatAction(observers)

        val contentItems = (observers.uiStates.last() as Content).items
        val ignoreThreatButton = contentItems.filterIsInstance<ActionButtonState>().first()
        assertThat(ignoreThreatButton.isEnabled).isEqualTo(false)
    }

    @Test
    fun `given failure response, when threat action fails, then action buttons are enabled`() = test {
        whenever(ignoreThreatUseCase.ignoreThreat(any(), any())).thenReturn(Failure.RemoteRequestFailure)
        val observers = init()

        triggerIgnoreThreatAction(observers)

        val contentItems = (observers.uiStates.last() as Content).items
        val ignoreThreatButton = contentItems.filterIsInstance<ActionButtonState>().first()
        assertThat(ignoreThreatButton.isEnabled).isEqualTo(true)
    }

    @Test
    fun `given success response, when ignore threat action is triggered, then update scan state action is triggered`() =
            test {
                whenever(ignoreThreatUseCase.ignoreThreat(any(), any())).thenReturn(Success)
                val observers = init()

                triggerIgnoreThreatAction(observers)

                assertThat(observers.navigation.last().peekContent())
                        .isEqualTo(ShowUpdatedScanStateWithMessage(R.string.threat_ignore_success_message))
            }

    private fun triggerIgnoreThreatAction(observers: Observers) {
        (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().last().onClick.invoke()
        (observers.navigation.last().peekContent() as OpenThreatActionDialog).okButtonAction.invoke()
    }

    private fun triggerFixThreatAction(observers: Observers) {
        (observers.uiStates.last() as Content).items.filterIsInstance<ActionButtonState>().first().onClick.invoke()
        (observers.navigation.last().peekContent() as OpenThreatActionDialog).okButtonAction.invoke()
    }

    private fun createDummyThreatDetailsListItems(
        primaryAction: (() -> Unit)? = null,
        secondaryAction: (() -> Unit)? = null,
        onEnterServerCredsIconClicked: (() -> Unit)? = null
    ): List<JetpackListItemState> {
        val items = ArrayList<JetpackListItemState>()
        primaryAction?.let {
            items.add(
                    ActionButtonState(
                            text = fakeUiStringText,
                            contentDescription = fakeUiStringText,
                            isSecondary = false,
                            onClick = primaryAction
                    )
            )
        }
        secondaryAction?.let {
            items.add(
                    ActionButtonState(
                            text = fakeUiStringText,
                            contentDescription = fakeUiStringText,
                            isSecondary = true,
                            onClick = secondaryAction
                    )
            )
        }
        onEnterServerCredsIconClicked?.let {
            items.add(
                    FootnoteState(
                            iconResId = R.drawable.ic_plus_white_24dp,
                            iconColorResId = R.color.primary,
                            text = UiStringText(SERVER_CREDS_LINK),
                            onIconClick = onEnterServerCredsIconClicked
                    )
            )
        }
        return items
    }

    private fun init(): Observers {
        val uiStates = mutableListOf<UiState>()
        viewModel.uiState.observeForever {
            uiStates.add(it)
        }
        val snackbarMsgs = mutableListOf<Event<SnackbarMessageHolder>>()
        viewModel.snackbarEvents.observeForever {
            snackbarMsgs.add(it)
        }
        val navigation = mutableListOf<Event<ThreatDetailsNavigationEvents>>()
        viewModel.navigationEvents.observeForever {
            navigation.add(it)
        }

        viewModel.start(threatId)

        return Observers(uiStates, snackbarMsgs, navigation)
    }

    private data class Observers(
        val uiStates: List<UiState>,
        val snackBarMsgs: List<Event<SnackbarMessageHolder>>,
        val navigation: List<Event<ThreatDetailsNavigationEvents>>
    )
}
