package org.wordpress.android.ui.jpfullplugininstall.onboarding.compose.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppTheme

@Composable
fun PluginDescription(
    siteName: String,
    pluginName: String,
) {
    Text(
        text = buildAnnotatedString {
            val fullJpPluginText =
                stringResource(R.string.jetpack_full_plugin_install_onboarding_description_full_jetpack_plugin)
            val text = String.format(
                stringResource(R.string.jetpack_full_plugin_install_onboarding_description),
                siteName,
                pluginName,
                fullJpPluginText,
            )
            val indexTextList = mutableListOf<PluginDescriptionTextPart>()
            indexTextList.add(PluginDescriptionTextPart(text.indexOf(pluginName), pluginName, true))
            indexTextList.add(PluginDescriptionTextPart(text.indexOf(siteName), siteName, true))
            indexTextList.add(PluginDescriptionTextPart(text.indexOf(fullJpPluginText), fullJpPluginText, true))
            text.split(pluginName, siteName, fullJpPluginText)
                .filter { it.isNotEmpty() }
                .forEach {
                    indexTextList.add(PluginDescriptionTextPart(text.indexOf(it), it, false))
                }
            indexTextList.sortedBy { it.index }.forEach {
                if (it.isBold) appendBold(it.text) else append(it.text)
            }
        },
        fontSize = 17.sp,
        style = TextStyle(letterSpacing = (-0.01).sp),
        modifier = Modifier
            .padding(horizontal = 30.dp)
            .padding(top = 20.dp)
    )
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
private fun PreviewPluginDescription() {
    AppTheme {
        PluginDescription(
            siteName = "wordpress.com",
            pluginName = "Jetpack Backup",
        )
    }
}
