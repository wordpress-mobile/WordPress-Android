package org.wordpress.android.ui.people

import org.wordpress.android.ui.people.InviteLinksUiStateType.LOADING

data class InviteLinksUiState(
    val type: InviteLinksUiStateType,
    val isLinksSectionVisible: Boolean,
    val loadAndRetryUiState: LoadAndRetryUiState,
    val isShimmerSectionVisible: Boolean,
    val isRoleSelectionAllowed: Boolean,
    val links: List<InviteLinksUiItem>,
    val inviteLinksSelectedRole: InviteLinksUiItem,
    val enableManageLinksActions: Boolean,
    val startShimmer: Boolean = isShimmerSectionVisible && type == LOADING,
    val isActionButtonsEnabled: Boolean = !startShimmer
)

enum class InviteLinksUiStateType {
    HIDDEN,
    LOADING,
    GET_STATUS_RETRY,
    LINKS_GENERATE,
    LINKS_AVAILABLE
}

enum class LoadAndRetryUiState {
    HIDDEN,
    LOADING,
    RETRY
}

data class InviteLinksUiItem(val roleName: String, val roleDisplayName: String, val expiryDate: String) {
    companion object {
        fun getEmptyItem() = InviteLinksUiItem("", "", "-")
    }
}
