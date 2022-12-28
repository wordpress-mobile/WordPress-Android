package org.wordpress.android.ui.reader.repository

import org.wordpress.android.ui.reader.services.discover.ReaderDiscoverLogic.DiscoverTasks

sealed class ReaderRepositoryEvent {
    object ReaderPostTableActionEnded : ReaderRepositoryEvent()
}

sealed class ReaderRepositoryCommunication {
    object Started : ReaderRepositoryCommunication()
    object Success : ReaderRepositoryCommunication()
    data class SuccessWithData<out T>(val data: T) : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        object RemoteRequestFailure : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}

sealed class ReaderDiscoverCommunication {
    abstract val task: DiscoverTasks

    data class Started(override val task: DiscoverTasks) : ReaderDiscoverCommunication()
    data class Success(override val task: DiscoverTasks) : ReaderDiscoverCommunication()
    sealed class Error : ReaderDiscoverCommunication() {
        data class NetworkUnavailable(override val task: DiscoverTasks) : Error()
        data class RemoteRequestFailure(override val task: DiscoverTasks) : Error()
        data class ServiceNotStarted(override val task: DiscoverTasks) : Error()
    }
}
