package org.wordpress.android.support.he.model

sealed class MessageSendResult {
    data object Success : MessageSendResult()
    data object Failure : MessageSendResult()
}
