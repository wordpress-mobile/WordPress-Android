package org.wordpress.android.ui.reader.adapters

fun interface OnSuggestionClickListener {
    fun onSuggestionClicked(query: String?)
}

fun interface OnSuggestionDeleteClickListener {
    fun onDeleteClicked(query: String?)
}

fun interface OnSuggestionClearClickListener {
    fun onClearClicked()
}
