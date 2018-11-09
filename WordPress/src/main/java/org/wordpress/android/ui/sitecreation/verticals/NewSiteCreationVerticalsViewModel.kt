package org.wordpress.android.ui.sitecreation.verticals

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withContext
import org.apache.commons.lang3.StringUtils
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.vertical.VerticalModel
import org.wordpress.android.fluxc.store.VerticalStore.OnVerticalsFetched
import org.wordpress.android.models.networkresource.ListState
import org.wordpress.android.models.networkresource.ListState.Error
import org.wordpress.android.models.networkresource.ListState.Loading
import org.wordpress.android.modules.IO_DISPATCHER
import org.wordpress.android.modules.MAIN_DISPATCHER
import org.wordpress.android.ui.sitecreation.usecases.FetchVerticalsUseCase
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsHeaderUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsModelUiState
import org.wordpress.android.ui.sitecreation.verticals.NewSiteCreationVerticalsViewModel.VerticalsListItemUiState.VerticalsSearchInputUiState
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
        updateUiState("", ListState.Ready(emptyList()))
    }

    fun updateQuery(query: String) {
        fetchVerticals(query)
    }

    private fun fetchVerticals(query: String) {
        launch {
            withContext(MAIN) {
                updateUiState(query, ListState.Loading(listState, false))
            }
            val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
            withContext(MAIN) {
                onVerticalsFetched(query, fetchedVerticals)
            }
        }
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
