package org.wordpress.android.ui.pagesrs

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

internal sealed interface PageRsListEvent {
    data class EditPage(
        val site: SiteModel,
        val page: PostModel
    ) : PageRsListEvent

    data object CreateNewPage : PageRsListEvent

    data class ShowToast(
        val messageResId: Int
    ) : PageRsListEvent

    data object Finish : PageRsListEvent
}
