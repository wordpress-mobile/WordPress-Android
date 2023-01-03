package org.wordpress.android.ui.reader.discover

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.PagePostCreationSourcesDetail
import org.wordpress.android.ui.engagement.HeaderData
import org.wordpress.android.ui.main.SitePickerAdapter.SitePickerMode
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource

sealed class ReaderNavigationEvents {
    data class ShowPostDetail(val post: ReaderPost) : ReaderNavigationEvents()
    data class SharePost(val post: ReaderPost) : ReaderNavigationEvents()
    data class OpenPost(val post: ReaderPost) : ReaderNavigationEvents()
    data class ShowPostsByTag(val tag: ReaderTag) : ReaderNavigationEvents()
    data class ShowReaderComments(
        val blogId: Long,
        val postId: Long,
        val source: ThreadedCommentsActionSource
    ) : ReaderNavigationEvents()

    object ShowNoSitesToReblog : ReaderNavigationEvents()
    data class ShowSitePickerForResult(val preselectedSite: SiteModel, val post: ReaderPost, val mode: SitePickerMode) :
        ReaderNavigationEvents()

    data class OpenEditorForReblog(
        val site: SiteModel,
        val post: ReaderPost,
        val source: PagePostCreationSourcesDetail
    ) : ReaderNavigationEvents()

    object ShowBookmarkedTab : ReaderNavigationEvents()
    class ShowBookmarkedSavedOnlyLocallyDialog(val okButtonAction: () -> Unit) : ReaderNavigationEvents() {
        @StringRes
        val title: Int = R.string.reader_save_posts_locally_dialog_title

        @StringRes
        val message: Int = R.string.reader_save_posts_locally_dialog_message

        @StringRes
        val buttonLabel: Int = R.string.dialog_button_ok
    }

    data class ShowVideoViewer(val videoUrl: String) : ReaderNavigationEvents()
    data class ShowBlogPreview(
        val siteId: Long,
        val feedId: Long,
        val isFollowed: Boolean
    ) : ReaderNavigationEvents()

    data class ShowReportPost(val url: String) : ReaderNavigationEvents()
    data class ShowReportUser(val url: String, val authorId: Long) : ReaderNavigationEvents()
    object ShowReaderSubs : ReaderNavigationEvents()
    data class ShowRelatedPostDetails(val postId: Long, val blogId: Long) :
        ReaderNavigationEvents()

    data class ReplaceRelatedPostDetailsWithHistory(val postId: Long, val blogId: Long, val isGlobal: Boolean) :
        ReaderNavigationEvents()

    data class ShowMediaPreview(val site: SiteModel?, val featuredImage: String) : ReaderNavigationEvents()
    data class OpenUrl(val url: String) : ReaderNavigationEvents()
    data class ShowPostInWebView(val post: ReaderPost) : ReaderNavigationEvents()
    data class ShowEngagedPeopleList(
        val siteId: Long,
        val postId: Long,
        val headerData: HeaderData
    ) : ReaderNavigationEvents()
}
