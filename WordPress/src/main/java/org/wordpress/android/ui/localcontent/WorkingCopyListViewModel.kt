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
import javax.inject.Inject
import javax.inject.Named

/** Lists the working copies of one [WorkingCopyKind] on every site, newest saved first. */
@HiltViewModel
class WorkingCopyListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serviceProvider: LocalContentServiceProvider,
    private val rowMapper: WorkingCopyRowMapper,
    private val appLogWrapper: AppLogWrapper,
    @Named(IO_THREAD) private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val kind = WorkingCopyKind.valueOf(
        requireNotNull(savedStateHandle[EXTRA_KIND]) { "Missing $EXTRA_KIND in SavedStateHandle" }
    )

    private val _uiState = MutableStateFlow<WorkingCopyListUiState>(WorkingCopyListUiState.Loading)
    val uiState: StateFlow<WorkingCopyListUiState> = _uiState.asStateFlow()

    /** Reads the working copies, keeping the rows on screen until the read finishes. */
    fun load() {
        viewModelScope.launch {
            _uiState.value = try {
                WorkingCopyListUiState.Loaded(withContext(ioDispatcher) { readRows() })
            } catch (e: LocalContentException) {
                appLogWrapper.e(T.POSTS, "Local content: couldn't read the $kind working copies: $e")
                WorkingCopyListUiState.Error(e.toString())
            }
        }
    }

    private fun readRows(): List<WorkingCopyRowUiState> {
        val service = serviceProvider.getService()
        return when (kind) {
            WorkingCopyKind.POST -> service.readAllPostWorkingCopies().map {
                rowMapper.toRow(it.id, it.fields.common, it.remoteTarget, it.updatedAt, isSticky = it.fields.sticky)
            }
            WorkingCopyKind.PAGE -> service.readAllPageWorkingCopies().map {
                rowMapper.toRow(it.id, it.fields.common, it.remoteTarget, it.updatedAt, isSticky = false)
            }
        }
    }

    companion object {
        const val EXTRA_KIND = "kind"
    }
}
