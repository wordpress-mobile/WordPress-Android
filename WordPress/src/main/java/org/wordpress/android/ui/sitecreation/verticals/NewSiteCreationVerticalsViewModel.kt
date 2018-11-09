package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import javax.inject.Inject
import javax.inject.Named
import kotlin.coroutines.experimental.CoroutineContext

private const val throttleDelay: Int = 500

class NewSiteCreationVerticalsViewModel @Inject constructor(
    private val dispatcher: Dispatcher,
    private val fetchVerticalsUseCase: FetchVerticalsUseCase,
    @Named(IO_DISPATCHER) private val IO: CoroutineContext,
    @Named(MAIN_DISPATCHER) private val MAIN: CoroutineContext
) : ViewModel(),
        CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = IO + job
    private var isStarted = false

    private val _uiState: MutableLiveData<VerticalsUiState> = MutableLiveData()
    val uiState: LiveData<VerticalsUiState> = _uiState

    private lateinit var listState: ListState<VerticalModel>

    init {
        dispatcher.register(fetchVerticalsUseCase)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(fetchVerticalsUseCase)
    }

    fun start() {
        if (isStarted) {
            return
        }
        isStarted = true
    }

    data class VerticalsUiState(
        val showError: Boolean,
        val showContent: Boolean,
        val showSkipButton: Boolean,
        val items: List<VerticalsListItemUiState>
    )

    sealed class VerticalsListItemUiState {
        data class VerticalsHeaderUiState(val title: String, val subtitle: String) : VerticalsListItemUiState()
        data class VerticalsSearchInputUiState(val showProgress: Boolean, val showClearButton: Boolean) :
                VerticalsListItemUiState()

        data class VerticalsModelUiState(val id: String, val title: String) : VerticalsListItemUiState()
        data class VerticalsNewModelUiState(val title: String, val subtitleResId: Int) :
                VerticalsListItemUiState()
    }
}
