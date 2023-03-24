package org.wordpress.android.ui.jetpackoverlay.individualplugin.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.utils.htmlToAnnotatedString
import org.wordpress.android.ui.jetpackoverlay.individualplugin.SiteWithIndividualJetpackPlugins

@Composable
fun MultipleSitesContent(
    sites: List<SiteWithIndividualJetpackPlugins>
) {
    Text(
        text = stringResource(R.string.wp_jetpack_individual_plugin_overlay_multiple_sites_content_1),
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.wp_jetpack_individual_plugin_overlay_content_2)
    )
    Spacer(modifier = Modifier.height(16.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        sites.forEach { site ->
            MultipleSitesContentItem(site = site)
        }
    }
}

@Composable
private fun MultipleSitesContentItem(
    site: SiteWithIndividualJetpackPlugins
) {
    val text = if (site.individualPluginNames.size > 1) {
        stringResource(
            R.string.wp_jetpack_individual_plugin_overlay_multiple_sites_content_item_multiple_plugins,
            site.url,
            site.individualPluginNames.size,
        )
    } else {
        stringResource(
            R.string.wp_jetpack_individual_plugin_overlay_multiple_sites_content_item_single_plugin,
            site.url,
            site.individualPluginNames.firstOrNull().orEmpty(),
        )
    }

    Text(
        text = htmlToAnnotatedString(text),
        modifier = Modifier.padding(vertical = 12.dp)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalContentColor.current.copy(alpha = 0.1f))
    )
}
