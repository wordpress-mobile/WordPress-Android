package org.wordpress.android.ui.main

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.mysite.SelectedSiteRepository

class SitePickerContract (private val siteStoreProvider: () -> SiteStore) : ActivityResultContract<Unit, SiteModel?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(context, ChooseSiteActivity::class.java).apply {
            putExtra(ChooseSiteActivity.KEY_SITE_PICKER_MODE, SitePickerMode.WPCOM_SITES_ONLY.name)
        }

    override fun parseResult(resultCode: Int, intent: Intent?) =
        if (resultCode == AppCompatActivity.RESULT_OK) {
            intent?.getIntExtra(
                ChooseSiteActivity.KEY_SITE_LOCAL_ID,
                SelectedSiteRepository.UNAVAILABLE,
            )?.let { siteLocalId ->
                siteStoreProvider().getSiteByLocalId(siteLocalId)
            }
        } else {
            null
        }
}
