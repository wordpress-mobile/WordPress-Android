package org.wordpress.android.ui.posts

import android.content.Context
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.support.ZendeskExtraTags
import org.wordpress.android.support.ZendeskHelper
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.accounts.HelpActivity.Origin.EDITOR_HELP
import org.wordpress.android.util.SiteUtils

object EditPostCustomerSupportHelper {
    fun onContactCustomerSupport(zendeskHelper: ZendeskHelper, context: Context, site: SiteModel) {
        zendeskHelper.createNewTicket(context, EDITOR_HELP, site, getTagsList(site))
    }

    fun onGotoCustomerSupportOptions(context: Context, site: SiteModel) {
        ActivityLauncher.viewHelpAndSupport(context, EDITOR_HELP, site, getTagsList(site))
    }

    private fun getTagsList(site: SiteModel): List<String>? =
        // Append the "mobile_gutenberg_is_default" tag if gutenberg is set to default for new posts
        if (SiteUtils.isBlockEditorDefaultForNewPost(site)) {
            listOf(ZendeskExtraTags.gutenbergIsDefault)
        } else {
            null
        }
}
