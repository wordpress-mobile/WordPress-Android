package org.wordpress.android.ui.reader.repository

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
