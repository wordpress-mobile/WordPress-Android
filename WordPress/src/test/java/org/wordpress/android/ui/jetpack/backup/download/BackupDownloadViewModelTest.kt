package org.wordpress.android.ui.jetpack.backup.download

import android.os.Bundle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.clearInvocations
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.activity.ActivityLogModel
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.DownloadFile
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadNavigationEvents.ShareLink
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.CompleteState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.DetailsState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ContentState.ProgressState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadUiState.ErrorState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCanceled
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadCompleted
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadViewModel.BackupDownloadWizardState.BackupDownloadInProgress
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.CompleteToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.DetailsToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.ErrorToolbarState
import org.wordpress.android.ui.jetpack.backup.download.ToolbarState.ProgressToolbarState
import org.wordpress.android.ui.jetpack.backup.download.builders.BackupDownloadStateListItemBuilder
import org.wordpress.android.ui.jetpack.backup.download.usecases.GetBackupDownloadStatusUseCase
import org.wordpress.android.ui.jetpack.backup.download.usecases.PostBackupDownloadUseCase
import org.wordpress.android.ui.jetpack.common.CheckboxSpannableLabel
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.ActionButtonState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState.CheckboxState
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider
import org.wordpress.android.ui.jetpack.usecases.GetActivityLogItemUseCase
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.text.PercentFormatter
import org.wordpress.android.util.wizard.WizardManager
import org.wordpress.android.util.wizard.WizardNavigationTarget
import org.wordpress.android.viewmodel.SingleLiveEvent
import java.util.Date

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
class BackupDownloadViewModelTest : BaseUnitTest() {
    @Mock lateinit var wizardManager: WizardManager<BackupDownloadStep>
    @Mock lateinit var backupDownloadStep: BackupDownloadStep
    @Mock lateinit var savedInstanceState: Bundle
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var getActivityLogItemUseCase: GetActivityLogItemUseCase
    @Mock private lateinit var backupDownloadStatusUseCase: GetBackupDownloadStatusUseCase
    @Mock private lateinit var postBackupDownloadUseCase: PostBackupDownloadUseCase
    @Mock private lateinit var checkboxSpannableLabel: CheckboxSpannableLabel
    @Mock private lateinit var percentFormatter: PercentFormatter
    private lateinit var availableItemsProvider: JetpackAvailableItemsProvider
    private lateinit var stateListItemBuilder: BackupDownloadStateListItemBuilder

    private val wizardManagerNavigatorLiveData = SingleLiveEvent<BackupDownloadStep>()

    private lateinit var viewModel: BackupDownloadViewModel

    private val activityId = "1"

    private val backupDownloadState = BackupDownloadState(
            activityId = "activityId",
            rewindId = "rewindId",
            downloadId = 100L,
            siteId = 200L,
            url = null,
            published = Date(1609690147756)
    )

    @Before
    fun setUp() = test {
        whenever(wizardManager.navigatorLiveData).thenReturn(wizardManagerNavigatorLiveData)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = backupDownloadStep
            Unit
        }
        availableItemsProvider = JetpackAvailableItemsProvider()
        stateListItemBuilder = BackupDownloadStateListItemBuilder(checkboxSpannableLabel, percentFormatter)
        viewModel = BackupDownloadViewModel(
                wizardManager,
                availableItemsProvider,
                getActivityLogItemUseCase,
                stateListItemBuilder,
                postBackupDownloadUseCase,
                backupDownloadStatusUseCase,
                TEST_DISPATCHER,
                percentFormatter
        )
        whenever(getActivityLogItemUseCase.get(anyOrNull())).thenReturn(fakeActivityLogModel)
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadSuccess)
        whenever(backupDownloadStatusUseCase.getBackupDownloadStatus(anyOrNull(), anyOrNull()))
                .thenReturn(flow { emit(getStatusProgress) })
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
    }

    @Test
    fun `given view model, when started, then process moves to next step`() = test {
        startViewModel()

        verify(wizardManager).showNextStep()
    }

    @Test
    fun `given view model, when started, then state reflect details`() = test {
        val uiStates = initObservers().uiStates

        startViewModel()

        assertThat(uiStates.last()).isInstanceOf(DetailsState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `given item is checked, when item is clicked, then item gets unchecked`() = test {
        val uiStates = initObservers().uiStates

        startViewModel()

        clearInvocations(wizardManager)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat((((uiStates.last()).items)
                .first { it is CheckboxState } as CheckboxState).checked).isFalse
    }

    @Test
    fun `given item is unchecked, when item is clicked, then item gets checked`() = test {
        val uiStates = initObservers().uiStates

        startViewModel()

        clearInvocations(wizardManager)

        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()
        ((uiStates.last().items).first { it is CheckboxState } as CheckboxState).onClick.invoke()

        assertThat(((uiStates.last().items).first { it is CheckboxState } as CheckboxState).checked).isTrue
    }

    @Test
    fun `given details step, when request create download success, then state reflects progress`() = test {
        whenever(percentFormatter.format(0)).thenReturn("30%")
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadSuccess)

        val uiStates = initObservers().uiStates

        startViewModel()
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.PROGRESS
            Unit
        }

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(uiStates.last()).isInstanceOf(ProgressState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(ProgressToolbarState::class.java)
    }

    @Test
    fun `given details step, when no network connection, then a snackbar message is shown`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadNetworkError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModel()

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.error_network_connection))
    }

    @Test
    fun `given details step, when request in error, then a snackbar message is shown`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadRemoteRequestError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModel()

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.backup_download_generic_failure))
    }

    @Test
    fun `given details step, when another request is running, then a snackbar message is shown`() = test {
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(otherRequestRunningError)

        val uiStates = initObservers().uiStates
        val msgs = initObservers().snackbarMessages

        startViewModel()

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(msgs.last().message).isEqualTo(UiStringRes(R.string.backup_download_another_download_running))
    }

    @Test
    fun `given in details step, when onBackPressed, then invokes wizard finished with cancel`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver

        startViewModel()
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.DETAILS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCanceled::class.java)
    }

    @Test
    fun `given in progress step, when onBackPressed, then invokes wizard finished with BackupDownloadInProgress`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        startViewModelForProgress()
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.PROGRESS.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadInProgress::class.java)
    }

    @Test
    fun `given in complete step, when onBackPressed, then invokes wizard finished with BackupDownloadCompleted`() {
        val wizardFinishedObserver = initObservers().wizardFinishedObserver
        startViewModelForComplete(backupDownloadState = backupDownloadState.copy(url = "www.wordpress.com"))
        clearInvocations(wizardManager)

        whenever(wizardManager.currentStep).thenReturn(BackupDownloadStep.COMPLETE.id)
        viewModel.onBackPressed()

        assertThat(wizardFinishedObserver.last()).isInstanceOf(BackupDownloadCompleted::class.java)
    }

    @Test
    fun `given backupDownloadState, when writeToBundle is invoked, state is writtenToBundle`() {
        startViewModel()

        viewModel.writeToBundle(savedInstanceState)

        verify(savedInstanceState)
                .putParcelable(any(), argThat { this is BackupDownloadState })
    }

    @Test
    fun `given step index, when returned from background, then step index is restored`() {
        val index = BackupDownloadStep.PROGRESS.id

        startViewModelForProgress()

        verify(wizardManager).setCurrentStepIndex(index)
    }

    @Test
    fun `given progress step, when started, then the progress is set to zero`() = test {
        whenever(percentFormatter.format(0)).thenReturn("30%")
        whenever(postBackupDownloadUseCase.postBackupDownloadRequest(anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(postBackupDownloadSuccess)

        val uiStates = initObservers().uiStates

        startViewModel()
        clearInvocations(wizardManager)
        whenever(wizardManager.showNextStep()).then {
            wizardManagerNavigatorLiveData.value = BackupDownloadStep.PROGRESS
            Unit
        }

        ((uiStates.last().items).first { it is ActionButtonState } as ActionButtonState).onClick.invoke()

        assertThat(((uiStates.last().items)
                .first { it is JetpackListItemState.ProgressState } as JetpackListItemState.ProgressState).progress)
                .isEqualTo(0)
    }

    @Test
    fun `given showStep for details is invoked, then state reflects details`() = test {
        val uiStates = initObservers().uiStates

        startViewModel()
        clearInvocations(wizardManager)

        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.DETAILS, backupDownloadState))

        assertThat(uiStates.last()).isInstanceOf(DetailsState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(DetailsToolbarState::class.java)
    }

    @Test
    fun `given showStep for progress is invoked, then state reflects progress`() = test {
        val uiStates = initObservers().uiStates

        startViewModelForProgress()
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.PROGRESS, backupDownloadState))

        assertThat(uiStates.last()).isInstanceOf(ProgressState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(ProgressToolbarState::class.java)
    }

    @Test
    fun `given showStep for progress is invoked, then call PercentFormatter`() = test {
        startViewModelForProgress()
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.PROGRESS, backupDownloadState))

        verify(percentFormatter).format(30)
    }

    @Test
    fun `given showStep for complete is invoked, then state reflects complete`() = test {
        val uiStates = initObservers().uiStates

        startViewModelForComplete(backupDownloadState = backupDownloadState.copy(url = "www.wordpress.com"))
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.COMPLETE, backupDownloadState))

        assertThat(uiStates.last()).isInstanceOf(CompleteState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(CompleteToolbarState::class.java)
    }

    @Test
    fun `given showStep for error is invoked, then state reflects error`() = test {
        val uiStates = initObservers().uiStates

        startViewModelForError()
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.ERROR, backupDownloadState))

        assertThat(uiStates.last()).isInstanceOf(ErrorState::class.java)
        assertThat(uiStates.last().toolbarState).isInstanceOf(ErrorToolbarState::class.java)
    }

    @Test
    fun `given complete, when downloadFile is clicked, then a navigationEvent is posted`() = test {
        val uiStates = initObservers().uiStates
        val navigationEvents = initObservers().navigationEvents
        val url = "www.google.com"

        startViewModelForComplete(backupDownloadState = backupDownloadState.copy(url = url))
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.COMPLETE, backupDownloadState))

        (uiStates.last().items)
                .filterIsInstance<ActionButtonState>()
                .first()
                .onClick.invoke()

        assertThat(navigationEvents.last()).isInstanceOf(DownloadFile::class.java)
    }

    @Test
    fun `given complete, when shareLink is clicked, then a navigationEvent is posted`() = test {
        val uiStates = initObservers().uiStates
        val navigationEvents = initObservers().navigationEvents
        val url = "www.google.com"

        startViewModelForComplete(backupDownloadState = backupDownloadState.copy(url = url))
        viewModel.showStep(WizardNavigationTarget(BackupDownloadStep.COMPLETE, backupDownloadState))

        (uiStates.last().items)
                .filterIsInstance<ActionButtonState>()
                .last()
                .onClick.invoke()

        assertThat(navigationEvents.last()).isInstanceOf(ShareLink::class.java)
    }

    private fun startViewModel(savedInstanceState: Bundle? = null) {
        viewModel.start(site, activityId, savedInstanceState)
    }

    private fun startViewModelForProgress() {
        whenever(savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP))
                .thenReturn(BackupDownloadStep.PROGRESS.id)
        whenever(savedInstanceState.getParcelable<BackupDownloadState>(KEY_BACKUP_DOWNLOAD_STATE))
                .thenReturn(backupDownloadState)
        whenever(percentFormatter.format(30))
                .thenReturn("30%")
        // Necessary to mock the call to BackupDownloadStateListItemBuilder
        whenever(percentFormatter.format(0))
                .thenReturn("0%")
        startViewModel(savedInstanceState)
    }

    private fun startViewModelForComplete(backupDownloadState: BackupDownloadState? = null) {
        whenever(savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP))
                .thenReturn(BackupDownloadStep.COMPLETE.id)
        whenever(savedInstanceState.getParcelable<BackupDownloadState>(KEY_BACKUP_DOWNLOAD_STATE))
                .thenReturn(backupDownloadState)
        startViewModel(savedInstanceState)
    }

    private fun startViewModelForError() {
        whenever(savedInstanceState.getInt(KEY_BACKUP_DOWNLOAD_CURRENT_STEP))
                .thenReturn(BackupDownloadStep.ERROR.id)
        whenever(savedInstanceState.getParcelable<BackupDownloadState>(KEY_BACKUP_DOWNLOAD_STATE))
                .thenReturn(backupDownloadState)
        startViewModel(savedInstanceState)
    }

    private fun initObservers(): Observers {
        val wizardFinishedObserver = mutableListOf<BackupDownloadWizardState>()
        viewModel.wizardFinishedObservable.observeForever { wizardFinishedObserver.add(it.peekContent()) }

        val snackbarMsgs = mutableListOf<SnackbarMessageHolder>()
        viewModel.snackbarEvents.observeForever { snackbarMsgs.add(it.peekContent()) }

        val navigationEvents = mutableListOf<BackupDownloadNavigationEvents>()
        viewModel.navigationEvents.observeForever { navigationEvents.add(it.peekContent()) }

        val uiStates = mutableListOf<BackupDownloadUiState>()
        viewModel.uiState.observeForever { uiStates.add(it) }

        return Observers(
                wizardFinishedObserver,
                snackbarMsgs,
                navigationEvents,
                uiStates
        )
    }

    private data class Observers(
        val wizardFinishedObserver: List<BackupDownloadWizardState>,
        val snackbarMessages: List<SnackbarMessageHolder>,
        val navigationEvents: List<BackupDownloadNavigationEvents>,
        val uiStates: List<BackupDownloadUiState>
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

    private val postBackupDownloadSuccess = BackupDownloadRequestState.Success(
            requestRewindId = "rewindId",
            rewindId = "rewindId",
            downloadId = 100L
    )

    private val getStatusProgress = BackupDownloadRequestState.Progress(
            rewindId = "rewindId",
            progress = 30
    )

    private val postBackupDownloadNetworkError = BackupDownloadRequestState.Failure.NetworkUnavailable
    private val postBackupDownloadRemoteRequestError = BackupDownloadRequestState.Failure.RemoteRequestFailure
    private val otherRequestRunningError = BackupDownloadRequestState.Failure.OtherRequestRunning
}
