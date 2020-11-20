package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction

@ActionEnum
enum class ScanAction : IAction {
    @Action
    FETCH_SCAN_STATE,
}
