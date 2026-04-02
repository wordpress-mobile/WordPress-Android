package org.wordpress.android.ui.reader.reblog

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost

sealed class ReblogState {
    class MultipleSites(val defaultSite: SiteModel, val post: ReaderPost) : ReblogState()
    class SingleSite(val site: SiteModel, val post: ReaderPost) : ReblogState()
    object NoSite : ReblogState()
    object Unknown : ReblogState()
}
