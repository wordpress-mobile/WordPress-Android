package org.wordpress.android.ui.posts

interface PostResolutionOverlayListener{
    // todo: annmarie - I am not sure if I want a single method with specific actions OR if I want to call
    // them out as methods. We shall see.
    fun onSaveAction(tag: String)
    fun onCancelAction(tag: String)
    fun onDismissAction(tag: String)
}
