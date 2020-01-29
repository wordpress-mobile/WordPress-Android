package org.wordpress.android.viewmodel.posts

sealed class PostListItemProgressBar(val visibility: Boolean) {
    object Hidden : PostListItemProgressBar(visibility = false)
    object Indeterminate : PostListItemProgressBar(visibility = true)
    data class Determinate(val progress: Int) : PostListItemProgressBar(visibility = true)
}
