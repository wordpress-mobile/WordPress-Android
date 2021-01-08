package org.wordpress.android.ui.reader.subfilter

import org.wordpress.android.ui.Organization

sealed class SubfilterVisibilityUiModel(val show: Boolean) {
    data class SubfilterShow(val organization: Organization) : SubfilterVisibilityUiModel(true)
    object SubfilterHide : SubfilterVisibilityUiModel(false)
}
