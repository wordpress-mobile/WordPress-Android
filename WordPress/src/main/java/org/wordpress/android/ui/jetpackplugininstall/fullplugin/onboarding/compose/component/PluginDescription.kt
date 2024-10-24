package org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.compose.component

import android.content.res.Configuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM2
import com.google.android.material.R as MaterialR

@Composable
fun PluginDescription(
    modifier: Modifier = Modifier,
    siteString: String,
    pluginNames: List<String>,
    useConciseText: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = buildPluginDescriptionText(pluginNames, siteString, useConciseText),
        style = dashboardCardDetail,
        color = colorResource(MaterialR.color.material_on_surface_emphasis_medium)
    )
}

val dashboardCardDetail = TextStyle(
        fontWeight = FontWeight.Normal,
)

@ReadOnlyComposable
@Composable
private fun buildPluginDescriptionText(
    pluginNames: List<String>,
    siteString: String,
    useConciseText: Boolean,
): AnnotatedString {
    val onboardingText = getOnboardingTextTemplate(pluginNames.size, useConciseText)
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
        onboardingText,
        siteString,
        pluginText,
        fullJpPluginText,
    )
    val indexTextList = mutableListOf<PluginDescriptionTextPart>()
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(pluginText), pluginText, true))
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(siteString), siteString, !useConciseText))
    indexTextList.add(PluginDescriptionTextPart(text.indexOf(fullJpPluginText), fullJpPluginText, true))

    return AnnotatedString.Builder(text).apply {
        indexTextList
            .filter { it.isBold }
            .forEach { addStyle(SpanStyle(fontWeight = FontWeight.Bold), it.index, it.index + it.text.length) }
    }.toAnnotatedString()
}

@ReadOnlyComposable
@Composable
private fun getOnboardingTextTemplate(pluginCount: Int, useConciseText: Boolean): String {
    return if (useConciseText) {
        if (pluginCount > 1) {
            stringResource(R.string.jetpack_full_plugin_install_concise_description_multiple)
        } else {
            stringResource(R.string.jetpack_full_plugin_install_concise_description_single)
        }
    } else {
        if (pluginCount > 1) {
            stringResource(R.string.jetpack_full_plugin_install_onboarding_description_multiple)
        } else {
            stringResource(R.string.jetpack_full_plugin_install_onboarding_description_single)
        }
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
    AppThemeM2 {
        PluginDescription(
            siteString = "wordpress.com",
            pluginNames = listOf("Jetpack Search"),
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionMultiplePlugins() {
    AppThemeM2 {
        PluginDescription(
            siteString = "wordpress.com",
            pluginNames = listOf("Jetpack Search", "Jetpack Protect"),
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionOnePluginConcise() {
    AppThemeM2 {
        PluginDescription(
            siteString = "This site",
            pluginNames = listOf("Jetpack Search"),
            useConciseText = true,
        )
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showBackground = true, device = Devices.PIXEL_4_XL, fontScale = 2f)
@Composable
private fun PreviewPluginDescriptionMultiplePluginsConcise() {
    AppThemeM2 {
        PluginDescription(
            siteString = "This site",
            pluginNames = listOf("Jetpack Search", "Jetpack Protect"),
            useConciseText = true,
        )
    }
}
