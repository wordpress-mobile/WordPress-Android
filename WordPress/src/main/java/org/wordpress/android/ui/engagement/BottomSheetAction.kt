package org.wordpress.android.ui.engagement

sealed class BottomSheetAction {
    object ShowBottomSheet : BottomSheetAction()
    object HideBottomSheet : BottomSheetAction()
}
