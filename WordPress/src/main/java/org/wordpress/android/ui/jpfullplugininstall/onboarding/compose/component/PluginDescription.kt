package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component

import android.content.res.Configuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun PluginDescription(
    modifier: Modifier = Modifier,
    siteName: String,
    pluginNames: List<String>,
) {
    Text(
        modifier = modifier,
        text = buildPluginDescriptionText(pluginNames, siteName),
        fontSize = 17.sp,
        style = TextStyle(letterSpacing = (-0.01).sp),
    )
}

@Composable
private fun buildPluginDescriptionText(
    pluginNames: List<String>,
    siteName: String
) = buildAnnotatedString {
    val pluginText = if (pluginNames.size > 1) {
        stringResource(R.string.jetpack_full_plugin_install_onboarding_description_multiple_plugins)
    } else {
        String.format(
            stringResource(R.string.jetpack_full_plugin_install_onboarding_description_single_plugin),
            pluginNames.firstOrNull().orEmpty()
        )
    }
    val fullJpPluginText =
        stringResource(R.string.jetpack_full_plugin_install_onboarding_description_full_jetpack_plugin)
    val text = String.format(
        stringResource(R.string.jetpack_full_plugin_install_onboarding_description),
        siteName,
        pluginText,
        fullJpPluginText,
    )
    val indexTextList = mutableListOf<PluginDescriptionTextPart>()
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(pluginText), pluginText, true))
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(siteName), siteName, true))
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(fullJpPluginText), fullJpPluginText, true))
    text.split(pluginText, siteName, fullJpPluginText)
        .filter { it.isNotEmpty() }
        .forEach {
            indexTextList.add(PluginDescriptionTextPart(text.indexOf(it), it, false))
        }
    indexTextList.sortedBy { it.index }.forEach {
        if (it.isBold) appendBold(it.text) else append(it.text)
    }
}

private fun AnnotatedString.Builder.appendBold(text: String) {
    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
        append(text)
    }
}

private data class PluginDescriptionTextPart(
    val index: Int,
    val text: String,
    val isBold: Boolean,
)

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionOnePlugin() {
    AppTheme {
        PluginDescription(
            siteName = "wordpress.com",
            pluginNames = listOf("Jetpack Search"),
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionMultiplePlugins() {
    AppTheme {
        PluginDescription(
            siteName = "wordpress.com",
            pluginNames = listOf("Jetpack Search", "Jetpack Protect"),
        )
    }
}
