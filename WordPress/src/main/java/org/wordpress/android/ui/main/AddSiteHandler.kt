package org.wordpress.android.ui.main

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import org.wordpress.android.BuildConfig
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource

/**
 * Helper class to handle adding a site.
 */
object AddSiteHandler {
    fun addSite(activity: FragmentActivity, hasAccessToken: Boolean, source: SiteCreationSource) {
        if (hasAccessToken) {
            if (!BuildConfig.ENABLE_ADD_SELF_HOSTED_SITE) {
                ActivityLauncher.newBlogForResult(activity, source)
            } else {
                // user is signed into wordpress app, so use the dialog to enable choosing whether to
                // create a new wp.com blog or add a self-hosted one
                showAddSiteDialog(activity, source)
            }
        } else {
            // user doesn't have an access token, so simply enable adding self-hosted
            ActivityLauncher.addSelfHostedSiteForResult(activity)
        }
    }

    private fun showAddSiteDialog(activity: FragmentActivity, source: SiteCreationSource) {
        val dialog: DialogFragment = AddSiteDialog()
        val args = Bundle()
        args.putString(ChooseSiteActivity.KEY_ARG_SITE_CREATION_SOURCE, source.label)
        dialog.arguments = args
        dialog.show(activity.supportFragmentManager, AddSiteDialog.ADD_SITE_DIALOG_TAG)
    }
}
