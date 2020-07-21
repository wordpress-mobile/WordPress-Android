package org.wordpress.android.ui.reader.repository

sealed class ReaderRepositoryCommunication {
    object Success : ReaderRepositoryCommunication()
    data class SuccessWithData<out T>(val data: T) : ReaderRepositoryCommunication()
    sealed class Error : ReaderRepositoryCommunication() {
        object NetworkUnavailable : Error()
        object RemoteRequestFailure : Error()
        class ReaderRepositoryException(val exception: Exception) : Error()
    }

    override fun toString(): String {
        return "${this.javaClass.simpleName})"
    }
}
