package org.wordpress.android.ui.reader.discover

import org.wordpress.android.models.ReaderPost

sealed class ReaderNavigationEvents {
    data class SharePost(val post: ReaderPost): ReaderNavigationEvents()
    data class OpenPost(val post: ReaderPost): ReaderNavigationEvents()
    data class ShowReaderComments(val blogId: Long, val postId: Long): ReaderNavigationEvents()
}
