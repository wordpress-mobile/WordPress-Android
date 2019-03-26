package org.wordpress.android.ui.sitecreation.misc

import android.content.Context
import android.support.v4.content.ContextCompat
import android.support.v7.content.res.AppCompatResources
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiHelpers

class SearchInputWithHeader(private val uiHelpers: UiHelpers, rootView: View, onClear: () -> Unit) {
    private val headerLayout = rootView.findViewById<ViewGroup>(R.id.header_layout)
    private val headerTitle = rootView.findViewById<TextView>(R.id.title)
    private val headerSubtitle = rootView.findViewById<TextView>(R.id.subtitle)
    private val searchInput = rootView.findViewById<EditText>(R.id.input)
    private val progressBar = rootView.findViewById<View>(R.id.progress_bar)
    private val clearAllButton = rootView.findViewById<View>(R.id.clear_all_btn)
    private val clearAllLayout = rootView.findViewById<View>(R.id.clear_all_layout)
    private val divider = rootView.findViewById<View>(R.id.divider)

    var onTextChanged: ((String) -> Unit)? = null

    init {
        val context = rootView.context
        val greyColor = ContextCompat.getColor(context, R.color.grey)

        val inputDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_search_white_24dp)
        inputDrawable?.setTint(greyColor)
        searchInput.setCompoundDrawablesRelativeWithIntrinsicBounds(inputDrawable, null, null, null)

        val clearButtonDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_close_white_24dp)
        clearButtonDrawable?.setTint(greyColor)
        clearAllButton.background = clearButtonDrawable

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

    fun updateHeader(context: Context, uiState: SiteCreationHeaderUiState?) {
        val headerShouldBeVisible = uiState != null
        if (!headerShouldBeVisible && headerLayout.visibility == View.VISIBLE) {
            headerLayout.animate().translationY(-headerLayout.height.toFloat())
        } else if (headerShouldBeVisible && headerLayout.visibility == View.GONE) {
            headerLayout.animate().translationY(0f)
        }
        uiState?.let {
            uiHelpers.updateVisibility(headerLayout, true)
            headerTitle.text = uiHelpers.getTextOfUiString(context, uiState.title)
            headerSubtitle.text = uiHelpers.getTextOfUiString(context, uiState.subtitle)
        } ?: uiHelpers.updateVisibility(headerLayout, false)
    }

    fun updateSearchInput(context: Context, uiState: SiteCreationSearchInputUiState) {
        searchInput.hint = uiHelpers.getTextOfUiString(context, uiState.hint)
        uiHelpers.updateVisibility(progressBar, uiState.showProgress)
        uiHelpers.updateVisibility(clearAllLayout, uiState.showClearButton)
        uiHelpers.updateVisibility(divider, uiState.showDivider)
    }
}
