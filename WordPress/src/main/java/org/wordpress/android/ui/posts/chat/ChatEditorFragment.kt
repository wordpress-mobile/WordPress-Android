package org.wordpress.android.ui.posts.chat

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.chat_editor_fragment.*
import org.wordpress.android.R

class ChatEditorFragment : Fragment() {
    lateinit var listener: ChatEditorListener

    companion object {
        const val CHAT_EDITOR_FRAGMENT_TAG = "CHAT_EDITOR_FRAGMENT_TAG"

        fun newInstance(listener: ChatEditorListener) = ChatEditorFragment().apply { this.listener = listener }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.chat_editor_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        attachButton.setOnClickListener { listener.onAddChatMedia() }
        sendButton.setOnClickListener { listener.onSend() }
    }
}
