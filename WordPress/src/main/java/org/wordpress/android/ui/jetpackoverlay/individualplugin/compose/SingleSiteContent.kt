package org.wordpress.android.ui.jetpackoverlay.individualplugin.compose

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.htmlToAnnotatedString
import org.wordpress.android.ui.jetpackoverlay.individualplugin.SiteWithIndividualJetpackPlugins

@Composable
fun SingleSiteContent(
    site: SiteWithIndividualJetpackPlugins
) {
    val firstParagraphContent = if (site.individualPluginNames.size > 1) {
        stringResource(
            R.string.wp_jetpack_individual_plugin_overlay_single_site_multiple_plugins_content_1,
            site.url,
        )
    } else {
        stringResource(
            R.string.wp_jetpack_individual_plugin_overlay_single_site_single_plugin_content_1,
            site.url,
            site.individualPluginNames.firstOrNull().orEmpty(),
        )
    }

    Text(htmlToAnnotatedString(firstParagraphContent))
    Spacer(modifier = Modifier.height(16.dp))
    Text(stringResource(R.string.wp_jetpack_individual_plugin_overlay_content_2))
}
