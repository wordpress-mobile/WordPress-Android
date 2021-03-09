package org.wordpress.android.ui.posts.chat

interface ChatEditorListener {
    fun onAddChatMedia(onMediaSelected: (media: List<Long>) -> Unit)
    fun onSend(content: String)
}
