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

    fun updateQuery(query: String, delay: Int = throttleDelay) {
        job.cancel() // cancel any previous requests
        fetchVerticals(query, delay)
    }

    private fun fetchVerticals(query: String, throttleDelay: Int) {
        launch {
            withContext(MAIN) {
                updateUiState(query, ListState.Loading(listState, false))
            }
            delay(throttleDelay)
            val fetchedVerticals = fetchVerticalsUseCase.fetchVerticals(query)
            withContext(MAIN) {
                onVerticalsFetched(query, fetchedVerticals)
            }
        }
    }

    private fun onVerticalsFetched(query: String, event: OnVerticalsFetched) {
        if (event.isError) {
            updateUiState(query, ListState.Error(listState, event.error.message))
        } else {
            updateUiState(query, ListState.Success(event.verticalList))
        }
    }

    private fun updateUiState(query: String, state: ListState<VerticalModel>) {
        listState = state
        _uiState.value = VerticalsUiState(
                showError = state is Error,
                showContent = state !is Error,
                showSkipButton = StringUtils.isEmpty(query),
                items = if (state is Error)
                    emptyList()
                else
                    createUiStates(query, showProgress = state is Loading, data = state.data)
        )
    }

    private fun createUiStates(
        query: String,
        showProgress: Boolean,
        data: List<VerticalModel>
    ): List<VerticalsListItemUiState> {
        val items: ArrayList<VerticalsListItemUiState> = ArrayList()
        if (shouldShowHeader(query)) {
            addHeaderUiState(items)
        }
        addSearchInputUiState(query, showProgress, items)
        addModelsUiState(data, items)
        // TODO unknown vertical item
        return items
    }

    private fun shouldShowHeader(query: String): Boolean {
        return StringUtils.isEmpty(query)
    }

    private fun addHeaderUiState(items: ArrayList<VerticalsListItemUiState>) {
        // TODO replace with data from the server response
        items.add(VerticalsHeaderUiState("dummyTitle", "dummySubtitle"))
    }

    private fun addSearchInputUiState(
        query: String,
        showProgress: Boolean,
        items: ArrayList<VerticalsListItemUiState>
    ) {
        items.add(VerticalsSearchInputUiState(showProgress, !StringUtils.isEmpty(query)))
    }

    private fun addModelsUiState(
        data: List<VerticalModel>,
        items: ArrayList<VerticalsListItemUiState>
    ) {
        data.forEach { model ->
            items.add(VerticalsModelUiState(model.verticalId, model.name))
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
        data class VerticalsUnknownVerticalUiState(val title: String, val subtitleResId: Int) :
                VerticalsListItemUiState()
    }
}
