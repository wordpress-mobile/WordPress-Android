package org.wordpress.android.ui.postsrs.terms

data class SelectableTerm(
    val id: Long,
    val name: String,
    val level: Int,
    val isSelected: Boolean,
)

data class TermSelectionUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val isLoadingMore: Boolean = false,
    val canLoadMore: Boolean = false,
    val error: String? = null,
    val terms: List<SelectableTerm> = emptyList(),
    val searchQuery: String = "",
    val isCreating: Boolean = false,
    val showAddDialog: Boolean = false,
    val parentOptions: List<ParentOption> = emptyList(),
    val isHierarchical: Boolean = false,
    val hasChanges: Boolean = false,
)

data class ParentOption(val id: Long, val name: String)

sealed interface TermSelectionEvent {
    data class FinishWithSelection(
        val selectedIds: List<Long>,
    ) : TermSelectionEvent

    data object Finish : TermSelectionEvent

    data class ShowSnackbar(
        val message: String,
    ) : TermSelectionEvent
}
