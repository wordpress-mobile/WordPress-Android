package org.wordpress.android.ui.reader.reblog

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.ReaderPost

/**
 * Represents the Reblog View State
 *
 * @property site the site to reblog to
 * @property post the post to be reblogged
 */
sealed class ReblogState(val site: SiteModel? = null, val post: ReaderPost? = null)

/**
 * Represents the Site Picker View State
 *
 * @property site the preselected site
 * @property post the post to be reblogged (saved for later)
 */
class SitePicker(site: SiteModel, post: ReaderPost) : ReblogState(site, post)

/**
 * Represents the Post Editor View State
 *
 * @property site the site to reblog to
 * @property post the post to be reblogged
 */
class PostEditor(site: SiteModel, post: ReaderPost) : ReblogState(site, post)

/**
 * Represents the No Site View State
 */
object NoSite : ReblogState()

/**
 * Represents the Error State
 */
object ReblogError : ReblogState()
