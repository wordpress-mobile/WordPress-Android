package org.wordpress.android.ui.sitecreation

import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.wordpress.android.R

class SearchInputWithHeader(rootView: View, onClear: () -> Unit) {
    private val headerLayout = rootView.findViewById<ViewGroup>(R.id.header_layout)
    private val headerTitle = rootView.findViewById<TextView>(R.id.title)
    private val headerSubtitle = rootView.findViewById<TextView>(R.id.subtitle)
    private val searchInput = rootView.findViewById<EditText>(R.id.input)
    private val progressBar = rootView.findViewById<View>(R.id.progress_bar)
    private val clearAllButton = rootView.findViewById<View>(R.id.clear_all_btn)

    var onTextChanged: ((String) -> Unit)? = null

    init {
        val context = rootView.context

        val drawable = AppCompatResources.getDrawable(context, R.drawable.ic_search_white_24dp)
        drawable?.setTint(ContextCompat.getColor(context, R.color.grey))
        searchInput.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
        clearAllButton.background = drawable

        val clearAllLayout = rootView.findViewById<View>(R.id.clear_all_layout)
        clearAllLayout.setOnClickListener {
            onClear()
        }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                onTextChanged?.invoke(s?.toString() ?: "")
            }
        })
    }

    fun setInputText(text: String) {
        // If the text hasn't changed avoid triggering the text watcher
        if (searchInput.text.toString() != text) {
            searchInput.setText(text)
        }
    }

    fun updateHeader(uiState: SiteCreationHeaderUiState?) {
        uiState?.let {
            if (headerLayout.visibility == View.VISIBLE) {
                headerLayout.animate().translationY(-headerLayout.height.toFloat())
            } else if (headerLayout.visibility == View.GONE) {
                headerLayout.animate().translationY(0f)
            }
            updateVisibility(headerLayout, true)
            headerTitle.text = uiState.title
            headerSubtitle.text = uiState.subtitle
        } ?: updateVisibility(headerLayout, false)
    }

    fun updateSearchInput(uiState: SiteCreationSearchInputUiState) {
        searchInput.hint = uiState.hint
        updateVisibility(progressBar, uiState.showProgress)
        updateVisibility(clearAllButton, uiState.showClearButton)
    }

    private fun updateVisibility(view: View, visible: Boolean) {
        view.visibility = if (visible) View.VISIBLE else View.GONE
    }
}
