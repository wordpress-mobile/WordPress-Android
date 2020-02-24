package org.wordpress.android.ui.reader.adapters

interface OnSuggestionClickListener {
    fun onSuggestionClicked(query: String?)
}

interface OnSuggestionDeleteClickListener {
    fun onDeleteClicked(query: String?)
}

interface OnSuggestionClearClickListener {
    fun onClearClicked()
}
