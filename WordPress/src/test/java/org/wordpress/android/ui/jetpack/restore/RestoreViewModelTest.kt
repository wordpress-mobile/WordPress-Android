package org.wordpress.android.ui.jetpack.restore

import android.os.Bundle
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.Constants
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.test
import org.wordpress.android.ui.jetpack.common.CheckboxSpannableLabel
import org.wordpress.android.ui.jetpack.common.JetpackBackupRestoreListItemState.FootnoteState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.CONTENTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.MEDIA_UPLOADS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.PLUGINS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.ROOTS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.SQLS
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType.THEMES
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.ShowJetpackSettings
import org.wordpress.android.ui.jetpack.restore.RestoreNavigationEvents.VisitSite
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.AwaitingCredentials
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Complete
import org.wordpress.android.ui.jetpack.restore.RestoreRequestState.Success
import org.wordpress.android.ui.jetpack.restore.RestoreStep.COMPLETE
import org.wordpress.android.ui.jetpack.restore.RestoreStep.DETAILS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.ERROR
import org.wordpress.android.ui.jetpack.restore.RestoreStep.PROGRESS
import org.wordpress.android.ui.jetpack.restore.RestoreStep.WARNING
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.CompleteState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.DetailsState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ContentState.ProgressState
import org.wordpress.android.ui.jetpack.restore.RestoreUiState.ErrorState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCanceled
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreCompleted
import org.wordpress.android.ui.jetpack.restore.RestoreViewModel.RestoreWizardState.RestoreInProgress
import org.wordpress.android.ui.jetpack.restore.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.restore.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.restore.builders.RestoreStateListItemBuilder
import org.wordpress.android.ui.jetpack.restore.usecases.GetRestoreStatusUseCase
import org.wordpress.android.ui.jetpack.restore.usecases.PostRestoreUseCase
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.HtmlMessageUtils
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.Date

private const val TEST_SITE_ID = 1L
private const val SERVER_CREDS_LINK = "${Constants.URL_JETPACK_SETTINGS}/$TEST_SITE_ID"
private const val SERVER_CREDS_MSG_WITH_CLICKABLE_LINK =
        "<a href=\"$SERVER_CREDS_LINK\">Enter your server credentials&lt</a> " +
                "to enable one click site restores from backups."

@InternalCoroutinesApi
class RestoreViewModelTest : BaseUnitTest() {
    @Mock lateinit var wizardManager: WizardManager<RestoreStep>
    @Mock lateinit var savedInstanceState: Bundle
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var getActivityLogItemUseCase: GetActivityLogItemUseCase
    @Mock private lateinit var restoreStatusUseCase: GetRestoreStatusUseCase
    @Mock private lateinit var postRestoreUseCase: PostRestoreUseCase
    @Mock private lateinit var checkboxSpannableLabel: CheckboxSpannableLabel
    @Mock private lateinit var htmlMessageUtils: HtmlMessageUtils
    @Mock private lateinit var percentFormatter: PercentFormatter
    private lateinit var availableItemsProvider: JetpackAvailableItemsProvider
    private lateinit var stateListItemBuilder: RestoreStateListItemBuilder

    private val activityId = "1"
    private val wizardManagerNavigatorLiveData = SingleLiveEvent<RestoreStep>()

    private lateinit var viewModel: RestoreViewModel

    private val restoreState = RestoreState(
            rewindId = "rewindId",
            restoreId = 100L,
            optionsSelected = listOf(
                    Pair(THEMES.id, true),
                    Pair(PLUGINS.id, true),
                    Pair(MEDIA_UPLOADS.id, true),
                    Pair(SQLS.id, true),
                    Pair(ROOTS.id, true),
                    Pair(CONTENTS.id, true)
            ),
            published = Date(1609690147756),
            shouldInitProgress = true
    )

    @Before
    fun setUp() = test {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)

        availableItemsProvider = JetpackAvailableItemsProvider()
        stateListItemBuilder = RestoreStateListItemBuilder(checkboxSpannableLabel, htmlMessageUtils, percentFormatter)
        viewModel = RestoreViewModel(
                wizardManager,
                availableItemsProvider,
                getActivityLogItemUseCase,
                stateListItemBuilder,
                postRestoreUseCase,
                restoreStatusUseCase,
                TEST_DISPATCHER,
                percentFormatter
        )
        whenever(getActivityLogItemUseCase.get(anyOrNull())).thenReturn(fakeActivityLogModel)
        whenever(checkboxSpannableLabel.buildSpannableLabel(R.string.backup_item_themes, null))
                .thenReturn("themes")
        whenever(checkboxSpannableLabel.buildSpannableLabel(R.string.backup_item_plugins, null))
                .thenReturn("plugins")
        whenever(checkboxSpannableLabel.buildSpannableLabel(R.string.backup_item_media_uploads, null))
                .thenReturn("uploads")
        whenever(
                checkboxSpannableLabel.buildSpannableLabel(
                        R.string.backup_item_roots,
                        R.string.backup_item_roots_hint
                )
        )
                .thenReturn("roots")
        whenever(
                checkboxSpannableLabel.buildSpannableLabel(
                        R.string.backup_item_contents,
                        R.string.backup_item_content_hint
                )
        )
                .thenReturn("contents")
        whenever(checkboxSpannableLabel.buildSpannableLabel(R.string.backup_item_sqls, R.string.backup_item_sqls_hint))
                .thenReturn("sqls")
        whenever(restoreStatusUseCase.getRestoreStatus(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(flowOf(AwaitingCredentials(false)))
    }

    @Test
    fun `given view model, when started, then process moves to next step`() = test {
        startViewModel()

        verify(wizardManager).showNextStep()
    }

    @Test
    fun `given view model, when started, then state reflect details`() = test {
        val uiStates = initObservers().uiStates

        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = DETAILS
            Unit
        }

        startViewModel()

        assertThat(uiStates.last()).isInstanceOf(DetailsState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `when server creds icon is clicked, then app opens site's jetpack settings link`() = test {
        val observers = initObservers()
        whenever(site.siteId).thenReturn(TEST_SITE_ID)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = DETAILS
            Unit
        }
        whenever(restoreStatusUseCase.getRestoreStatus(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(flowOf(AwaitingCredentials(true)))
        whenever(htmlMessageUtils.getHtmlMessageFromStringFormatResId(anyInt(), any()))
                .thenReturn(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK)

        startViewModel()
        (observers.uiStates.last() as DetailsState).items
                .filterIsInstance(FootnoteState::class.java)
                .firstOrNull { it.text == UiStringText(SERVER_CREDS_MSG_WITH_CLICKABLE_LINK) }
                ?.onIconClick
                ?.invoke()

        assertThat(observers.navigationEvents.last()).isEqualTo(ShowJetpackSettings(SERVER_CREDS_LINK))
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = DETAILS
            Unit
        }

        startViewModel()

        clearInvocations(wizardManager)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat((((uiStates.last()).items)
                .first { it is CheckboxState } as CheckboxState).checked).isFalse
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = DETAILS
            Unit
        }

        startViewModel()

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()
        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat(((uiStates.last().items).first { it is CheckboxState } as CheckboxState).checked).isTrue
    }

    @Test
    fun `given in details step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        startViewModel()
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(DETAILS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreCanceled::class.java)
    }

    @Test
    fun `given in warning step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        startViewModel()
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(WARNING.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreCanceled::class.java)
    }

    @Test
    fun `given in progress step, when onBackPressed, then invokes wizard finished with RestoreInProgress`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        startViewModelForStep(PROGRESS)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(PROGRESS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreInProgress::class.java)
    }

    @Test
    fun `given in complete step, when onBackPressed, then invokes wizard finished with RestoreCompleted`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        startViewModelForStep(step = COMPLETE)
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(COMPLETE.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(RestoreCompleted::class.java)
    }

    @Test
    fun `given RestoreState, when writeToBundle is invoked, state is writtenToBundle`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState)
                .putParcelable(any(), argThat { this is RestoreState })
    }

    @Test
    fun `given step index, when returned from background, then step index is restored`() {
        val index = PROGRESS.id

        startViewModelForStep(PROGRESS)

        verify(wizardManager).setCurrentStepIndex(index)
    }

    @Test
    fun `given showStep for details is invoked, then state reflects details`() = test {
        val uiStates = initObservers().uiStates

        startViewModel()
        clearInvocations(wizardManager)

        viewModel.showStep(WizardNavigationTarget(DETAILS, restoreState))

        assertThat(uiStates.last()).isInstanceOf(DetailsState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `given showStep for progress is invoked, then state reflects progress`() = test {
        val uiStates = initObservers().uiStates
        clearInvocations(wizardManager)

        whenever(percentFormatter.format(0)).thenReturn("0%")
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postSuccess)
        whenever(restoreStatusUseCase.getRestoreStatus(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(flowOf(Complete("Id", 100L, Date(1609690147756))))

        startViewModelForStep(PROGRESS)

        viewModel.showStep(WizardNavigationTarget(PROGRESS, restoreState))

        assertThat(uiStates.last()).isInstanceOf(ProgressState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(ProgressToolbarState::class.java)
    }

    @Test
    fun `given showStep for progress is invoked, then call PercentFormatter`() = test {
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postSuccess)
        whenever(percentFormatter.format(0)).thenReturn("0%")

        startViewModelForStep(PROGRESS)

        viewModel.showStep(WizardNavigationTarget(PROGRESS, restoreState))

        verify(percentFormatter).format(0)
    }

    @Test
    fun `given showStep for complete is invoked, then state reflects complete`() = test {
        val uiStates = initObservers().uiStates

        startViewModelForStep(step = COMPLETE)
        viewModel.showStep(WizardNavigationTarget(COMPLETE, restoreState))

        assertThat(uiStates.last()).isInstanceOf(CompleteState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(CompleteToolbarState::class.java)
    }

    @Test
    fun `given showStep for error is invoked, then state reflects error`() = test {
        val uiStates = initObservers().uiStates

        startViewModelForStep(ERROR)
        viewModel.showStep(WizardNavigationTarget(ERROR, restoreState))

        assertThat(uiStates.last()).isInstanceOf(ErrorState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(ErrorToolbarState::class.java)
    }

    @Test
    fun `given complete, when visitSite is clicked, then a navigationEvent is posted`() = test {
        val uiStates = initObservers().uiStates
        val navigationEvents = initObservers().navigationEvents
        val url = "www.google.com"
        whenever(site.url).thenReturn(url)

        startViewModelForStep(step = COMPLETE)
        viewModel.showStep(WizardNavigationTarget(COMPLETE, restoreState))

        (uiStates.last().items)
                .filterIsInstance<ActionButtonState>()
                .last()
                .onClick.invoke()

        assertThat(navigationEvents.last()).isInstanceOf(VisitSite::class.java)
    }

    @Test
    fun `given progress step, when no network connection, then a snackbar message is shown`() = test {
        whenever(percentFormatter.format(0)).thenReturn("0%")
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postRestoreNetworkError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModelForStep(PROGRESS)
        viewModel.showStep(WizardNavigationTarget(PROGRESS, restoreState))

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.error_network_connection))
    }

    @Test
    fun `given progress step, when remote request fails, then a snackbar message is shown`() = test {
        whenever(percentFormatter.format(0)).thenReturn("0%")
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postRestoreRemoteRequestError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModelForStep(PROGRESS)
        viewModel.showStep(WizardNavigationTarget(PROGRESS, restoreState))

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.restore_generic_failure))
    }

    @Test
    fun `given progress step, when another request is running, then a snackbar message is shown`() = test {
        whenever(percentFormatter.format(0)).thenReturn("0%")
        whenever(postRestoreUseCase.postRestoreRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(otherRequestRunningError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModelForStep(PROGRESS)
        viewModel.showStep(WizardNavigationTarget(PROGRESS, restoreState))

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.restore_another_process_running))
    }

    private fun startViewModel(savedInstanceState: Bundle? = null) {
        viewModel.start(site, activityId, savedInstanceState)
    }

    private fun startViewModelForStep(step: RestoreStep, restoreState: RestoreState? = null) {
        whenever(savedInstanceState.getInt(KEY_RESTORE_CURRENT_STEP))
                .thenReturn(step.id)
        whenever(savedInstanceState.getParcelable<RestoreState>(KEY_RESTORE_STATE))
                .thenReturn(restoreState ?: this.restoreState)
        startViewModel(savedInstanceState)
    }

    private fun initObservers(): Observers {
        val wizardFinishedObserver = mutableListOf<RestoreWizardState>()
        viewModel.wizardFinishedObservable.observeForever { wizardFinishedObserver.add(it.peekContent()) }

        val snackbarMsgs = mutableListOf<SnackbarMessageHolder>()
        viewModel.snackbarEvents.observeForever { snackbarMsgs.add(it.peekContent()) }

        val navigationEvents = mutableListOf<RestoreNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        val uiStates = mutableListOf<RestoreUiState>()
        viewModel.uiState.observeForever { uiStates.add(it) }

        return Observers(
                wizardFinishedObserver,
                snackbarMsgs,
                navigationEvents,
                uiStates
        )
    }

    private data class Observers(
        val wizardFinishedObserver: List<RestoreWizardState>,
        val snackbarMessages: List<SnackbarMessageHolder>,
        val navigationEvents: List<RestoreNavigationEvents>,
        val uiStates: List<RestoreUiState>
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

    private val postRestoreNetworkError = RestoreRequestState.Failure.NetworkUnavailable
    private val postRestoreRemoteRequestError = RestoreRequestState.Failure.RemoteRequestFailure
    private val otherRequestRunningError = RestoreRequestState.Failure.OtherRequestRunning
    private val postSuccess = Success(rewindId = "rewindId", requestRewindId = "rewindId", restoreId = 1L)
}
