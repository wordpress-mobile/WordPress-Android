package org.wordpress.android.ui.reader.discover

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode

sealed class ReaderNavigationEvents {
    data class SharePost(val post: ReaderPost) : ReaderNavigationEvents()
    data class OpenPost(val post: ReaderPost) : ReaderNavigationEvents()
    data class ShowReaderComments(val blogId: Long, val postId: Long) : ReaderNavigationEvents()
    object ShowNoSitesToReblog : ReaderNavigationEvents()
    data class ShowSitePickerForResult(val site: SiteModel, val post: ReaderPost, val mode: SitePickerMode) :
            ReaderNavigationEvents()
    data class OpenEditorForReblog(
        val site: SiteModel,
        val post: ReaderPost,
        val source: PagePostCreationSourcesDetail
    ) : ReaderNavigationEvents()
}
