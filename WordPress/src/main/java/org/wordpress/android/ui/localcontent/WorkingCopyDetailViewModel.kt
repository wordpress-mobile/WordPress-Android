package org.wordpress.android.ui.localcontent

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.modules.IO_THREAD
import org.wordpress.android.util.AppLog.T
import uniffi.wp_mobile_local_content.LocalContentException
import uniffi.wp_mobile_local_content.WorkingCopyId
import javax.inject.Inject
import javax.inject.Named

/** Shows one working copy's stored values, read-only. */
@HiltViewModel
class WorkingCopyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serviceProvider: LocalContentServiceProvider,
    private val detailMapper: WorkingCopyDetailMapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val kind = WorkingCopyKind.valueOf(
        requireNotNull(savedStateHandle[EXTRA_KIND]) { "Missing $EXTRA_KIND in SavedStateHandle" }
    )
    private val id: WorkingCopyId =
        requireNotNull(savedStateHandle[EXTRA_ID]) { "Missing $EXTRA_ID in SavedStateHandle" }

    private val _uiState = MutableStateFlow<WorkingCopyDetailUiState>(WorkingCopyDetailUiState.Loading)
    val uiState: StateFlow<WorkingCopyDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = try {
                withContext(ioDispatcher) { read() } ?: WorkingCopyDetailUiState.NotFound
            } catch (e: LocalContentException) {
                appLogWrapper.e(T.POSTS, "Local content: couldn't read working copy $id: $e")
                WorkingCopyDetailUiState.Error(e.toString())
            }
        }
    }

    private fun read(): WorkingCopyDetailUiState.Loaded? {
        val service = serviceProvider.getService()
        return when (kind) {
            WorkingCopyKind.POST -> service.readPostWorkingCopy(id)?.let { detailMapper.toUiState(it) }
            WorkingCopyKind.PAGE -> service.readPageWorkingCopy(id)?.let { detailMapper.toUiState(it) }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
        const val EXTRA_ID = "id"
    }
}
