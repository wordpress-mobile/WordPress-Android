package org.wordpress.android.ui.stats.refresh

sealed class StatsModuleActivateRequestState {
    object Success : StatsModuleActivateRequestState()
    sealed class Failure : StatsModuleActivateRequestState() {
        object NetworkUnavailable : StatsModuleActivateRequestState()
        object RemoteRequestFailure : StatsModuleActivateRequestState()
    }
}
