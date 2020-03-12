package org.wordpress.android.ui.sitecreation.verticals

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.SegmentPromptModel
import org.wordpress.android.fluxc.store.VerticalStore.OnSegmentPromptFetched
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.sitecreation.usecases.FetchSegmentPromptUseCase
import org.wordpress.android.ui.sitecreation.verticals.SiteCreationVerticalsViewModel.VerticalsUiState.VerticalsContentUiState
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.CoroutineContext

class SiteCreationVerticalsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val fetchSegmentPromptUseCase: FetchSegmentPromptUseCase,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ViewModel(), CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = bgDispatcher + job
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

    private lateinit var segmentPrompt: SegmentPromptModel

    private var segmentId: Long? = null

    private val _verticalSelected = SingleLiveEvent<String>()
    val verticalSelected: LiveData<String> = _verticalSelected

    private val _skipBtnClicked = SingleLiveEvent<Unit>()
    val skipBtnClicked: LiveData<Unit> = _skipBtnClicked

    private val _onHelpClicked = SingleLiveEvent<Unit>()
    val onHelpClicked: LiveData<Unit> = _onHelpClicked

    init {
        dispatcher.register(fetchSegmentPromptUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchSegmentPromptUseCase)
    }

    fun start(segmentId: Long) {
        if (isStarted) {
            return
        }
        this.segmentId = segmentId
        isStarted = true
        fetchSegmentsPrompt()
    }

    private fun fetchSegmentsPrompt() {
        launch {
            val onSegmentsPromptFetchedEvent = fetchSegmentPromptUseCase.fetchSegmentsPrompt(segmentId!!)
            withContext(mainDispatcher) {
                onSegmentsPromptFetched(onSegmentsPromptFetchedEvent)
            }
        }
    }

    private fun onSegmentsPromptFetched(event: OnSegmentPromptFetched) {
        segmentPrompt = event.prompt!!
        updateUiStateToContent()
    }

    fun onSkipStepBtnClicked() {
        _skipBtnClicked.call()
    }

    fun onHelpClicked() {
        _onHelpClicked.call()
    }

    private fun updateUiStateToContent() {
        updateUiState(
                VerticalsContentUiState(
                        showSkipButton = true
                )
        )
    }

    private fun updateUiState(uiState: VerticalsUiState) {
        _uiState.value = uiState
    }

    sealed class VerticalsUiState(
        val contentLayoutVisibility: Boolean
    ) {
        data class VerticalsContentUiState(
            val showSkipButton: Boolean
        ) : VerticalsUiState(
                contentLayoutVisibility = true
        )
    }
}
