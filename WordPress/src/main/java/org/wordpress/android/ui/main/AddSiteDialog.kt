package org.wordpress.android.ui.main

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource

/**
 * Dialog to prompt the user to add a site.
 */
class AddSiteDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val source =
            SiteCreationSource.fromString(requireArguments().getString(ChooseSiteActivity.KEY_ARG_SITE_CREATION_SOURCE))
        val items = arrayOf<CharSequence>(
            getString(R.string.site_picker_create_wpcom),
            getString(R.string.site_picker_add_self_hosted)
        )
        val builder = MaterialAlertDialogBuilder(requireActivity())
        builder.setTitle(R.string.site_picker_add_a_site)
        builder.setAdapter(
            ArrayAdapter(requireActivity(), R.layout.add_new_site_dialog_item, R.id.text, items)
        ) { _: DialogInterface?, which: Int ->
            if (which == 0) {
                ActivityLauncher.newBlogForResult(activity, source)
            } else {
                ActivityLauncher.addSelfHostedSiteForResult(activity)
            }
        }
        AnalyticsTracker.track(
            AnalyticsTracker.Stat.ADD_SITE_ALERT_DISPLAYED,
            mapOf(ChooseSiteActivity.KEY_SOURCE to source.label)
        )
        return builder.create()
    }

    companion object {
        const val ADD_SITE_DIALOG_TAG = "add_site_dialog"
    }
}
