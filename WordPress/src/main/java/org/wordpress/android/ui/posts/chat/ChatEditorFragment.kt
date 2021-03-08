package org.wordpress.android.ui.posts.chat

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.chat_editor_fragment.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

private const val BLOCK_LIST_CONTENT_DESCRIPTION = "block-list"
private const val SHOW_KEYBOARD_DELAY = 200L

class ChatEditorFragment : Fragment() {
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var listener: ChatEditorListener
    private lateinit var viewModel: ChatEditorViewModel

    private var editorScrollView: ScrollView? = null
        get() {
            if (field == null) {
                activity?.findViewById<ViewGroup>(R.id.gutenberg_container)?.let {
                    field = it.findByContentDescription(BLOCK_LIST_CONTENT_DESCRIPTION) as? ScrollView
                }
            }
            return field
        }

    companion object {
        const val CHAT_EDITOR_FRAGMENT_TAG = "CHAT_EDITOR_FRAGMENT_TAG"

        fun newInstance(listener: ChatEditorListener) = ChatEditorFragment().apply { this.listener = listener }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
            inflater.inflate(R.layout.chat_editor_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModel()
        setupActionListeners()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        (requireActivity().applicationContext as WordPress).component().inject(this)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(ChatEditorViewModel::class.java)
        viewModel.onNewContent.observe(viewLifecycleOwner, Observer { content ->
            listener.onSend(content)
            editorScrollView?.post {
                editorScrollView?.fullScroll(View.FOCUS_DOWN)
            }
        })
        viewModel.onClearInput.observe(viewLifecycleOwner, Observer {
            chatEditor.text.clear()
        })
        viewModel.onAttachRequest.observe(viewLifecycleOwner, Observer {
            listener.onAddChatMedia()
        })
    }

    private fun setupActionListeners() {
        attachButton.setOnClickListener { viewModel.onAttachButtonPressed() }
        sendButton.setOnClickListener { viewModel.onSendButtonPressed(chatEditor.text.toString()) }
        chatEditor.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                Handler().postDelayed({ ActivityUtils.showKeyboard(v) }, SHOW_KEYBOARD_DELAY)
            }
        }
    }
}
