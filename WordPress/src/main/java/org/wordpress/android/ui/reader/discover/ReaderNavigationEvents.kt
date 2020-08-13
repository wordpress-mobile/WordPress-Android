package org.wordpress.android.ui.reader.discover

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode

sealed class ReaderNavigationEvents {
    data class SharePost(val post: ReaderPost) : ReaderNavigationEvents()
    data class OpenPost(val post: ReaderPost) : ReaderNavigationEvents()
    data class ShowPostsByTag(val tag: ReaderTag) : ReaderNavigationEvents()
    data class ShowReaderComments(val blogId: Long, val postId: Long) : ReaderNavigationEvents()
    object ShowNoSitesToReblog : ReaderNavigationEvents()
    data class ShowSitePickerForResult(val site: SiteModel, val post: ReaderPost, val mode: SitePickerMode) :
            ReaderNavigationEvents()

    data class OpenEditorForReblog(
        val site: SiteModel,
        val post: ReaderPost,
        val source: PagePostCreationSourcesDetail
    ) : ReaderNavigationEvents()

    object ShowBookmarkedTab : ReaderNavigationEvents()
    class ShowBookmarkedSavedOnlyLocallyDialog(val okButtonAction: () -> Unit) : ReaderNavigationEvents() {
        @StringRes val title: Int = R.string.reader_save_posts_locally_dialog_title
        @StringRes val message: Int = R.string.reader_save_posts_locally_dialog_message
        @StringRes val buttonLabel: Int = R.string.dialog_button_ok
    }
}
