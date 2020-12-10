package org.wordpress.android.ui.jetpack.scan

sealed class ScanListItemState(val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    enum class ViewType(val id: Int) {
        SCAN_STATE(0) // TODO: ashiagr add different view types
    }
}
