package org.wordpress.android.ui.reader.reblog

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost

/**
 * Represents the Reblog View State
 */
sealed class ReblogState

/**
 * Represents the Site Picker View State
 *
 * @property site the preselected site
 * @property post the post to be reblogged (saved for later)
 */
class SitePicker(val site: SiteModel, val post: ReaderPost) : ReblogState()

/**
 * Represents the Post Editor View State
 *
 * @property site the site to reblog to
 * @property post the post to be reblogged
 */
class PostEditor(val site: SiteModel, val post: ReaderPost) : ReblogState()

/**
 * Represents the No Site View State
 */
object NoSite : ReblogState()

/**
 * Represents the Unknown/Error State
 */
object Unknown : ReblogState()
