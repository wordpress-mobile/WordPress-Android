package org.wordpress.android.ui.pagesrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

internal sealed interface PageRsListEvent {
    data class EditPage(
        val site: SiteModel,
        val page: PostModel
    ) : PageRsListEvent

    data object CreateNewPage : PageRsListEvent

    data class ViewPage(val url: String) : PageRsListEvent

    data class SharePage(
        val url: String,
        val title: String
    ) : PageRsListEvent

    data class CopyPageUrl(val url: String) : PageRsListEvent

    /** Opens the block-theme homepage in the Site Editor web view via WPWebViewActivity. */
    data class OpenSiteEditor(
        val url: String,
        val useWpComCredentials: Boolean
    ) : PageRsListEvent

    data class PromoteWithBlaze(
        val site: SiteModel,
        val page: PostModel
    ) : PageRsListEvent

    data class ShowToast(
        val messageResId: Int
    ) : PageRsListEvent

    data object Finish : PageRsListEvent
}
