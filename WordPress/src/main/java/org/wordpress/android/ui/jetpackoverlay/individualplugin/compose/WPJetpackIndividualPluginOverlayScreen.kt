package org.wordpress.android.ui.jetpackoverlay.individualplugin.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.components.buttons.ButtonSize
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.buttons.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.theme.JpColorPalette
import org.wordpress.android.ui.jetpackoverlay.individualplugin.SiteWithIndividualJetpackPlugins
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.onboarding.compose.component.JPInstallFullPluginAnimation

private val TitleTextStyle
    @ReadOnlyComposable
    @Composable
    get() = TextStyle(
        fontSize = 28.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight.Bold,
    )

private val ContentTextStyle
    @ReadOnlyComposable
    @Composable
    get() = TextStyle(
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    )

private val ContentMargin = 20.dp

@Composable
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun WPJetpackIndividualPluginOverlayScreen(
    sites: List<SiteWithIndividualJetpackPlugins>,
    onCloseClick: () -> Unit,
    onPrimaryButtonClick: () -> Unit,
    onSecondaryButtonClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = null,
                navigationIcon = NavigationIcons.CloseIcon,
                onNavigationIconClick = onCloseClick
            )
        }
    ) {
        val orientation = LocalConfiguration.current.orientation
        val isLandscape = remember(orientation) { orientation == Configuration.ORIENTATION_LANDSCAPE }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (isLandscape) it.verticalScroll(rememberScrollState()) else it
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .let {
                        if (!isLandscape) {
                            it
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        } else {
                            it
                        }
                    }
                    .padding(ContentMargin),
                verticalArrangement = Arrangement.Center,
            ) {
                // Icon
                JPInstallFullPluginAnimation(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .let {
                            if (isLandscape) it.height(48.dp) else it
                        }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title
                Text(
                    text = getTitle(siteCount = sites.size),
                    style = TitleTextStyle,
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                CompositionLocalProvider(LocalTextStyle provides ContentTextStyle) {
                    when {
                        sites.size > 1 -> MultipleSitesContent(sites)
                        sites.size == 1 -> SingleSiteContent(sites.first())
                    }
                }
            }

            // Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = ContentMargin)
                    .padding(horizontal = ContentMargin),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                PrimaryButton(
                    text = stringResource(R.string.wp_jetpack_individual_plugin_overlay_primary_button),
                    onClick = onPrimaryButtonClick,
                    buttonSize = ButtonSize.LARGE,
                    padding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = JpColorPalette().primary,
                        contentColor = AppColor.White,
                    ),
                )
                SecondaryButton(
                    text = stringResource(R.string.wp_jetpack_continue_without_jetpack),
                    onClick = onSecondaryButtonClick,
                    buttonSize = ButtonSize.LARGE,
                    padding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = JpColorPalette().primary,
                    ),
                )
            }
        }
    }
}

@ReadOnlyComposable
@Composable
private fun getTitle(siteCount: Int): String = if (siteCount > 1) {
    stringResource(R.string.wp_jetpack_individual_plugin_overlay_multiple_sites_title)
} else {
    stringResource(R.string.wp_jetpack_individual_plugin_overlay_single_site_title)
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 720, heightDp = 360)
@Composable
fun WPJetpackIndividualPluginOverlayScreenSingleSiteSinglePluginPreview() {
    AppThemeM2 {
        WPJetpackIndividualPluginOverlayScreen(
            sites = listOf(
                SiteWithIndividualJetpackPlugins(
                    name = "Site 1",
                    url = "site1.wordpress.com",
                    individualPluginNames = listOf("Jetpack Social")
                ),
            ),
            onCloseClick = {},
            onPrimaryButtonClick = {},
            onSecondaryButtonClick = {},
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 720, heightDp = 360)
@Composable
fun WPJetpackIndividualPluginOverlayScreenSingleSiteMultiplePluginsPreview() {
    AppThemeM2 {
        WPJetpackIndividualPluginOverlayScreen(
            sites = listOf(
                SiteWithIndividualJetpackPlugins(
                    name = "Site 1",
                    url = "site1.wordpress.com",
                    individualPluginNames = listOf("Jetpack Social", "Jetpack Search")
                ),
            ),
            onCloseClick = {},
            onPrimaryButtonClick = {},
            onSecondaryButtonClick = {},
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Preview(widthDp = 360, heightDp = 600)
@Preview(widthDp = 720, heightDp = 360)
@Composable
fun WPJetpackIndividualPluginOverlayScreenMultipleSitesPreview() {
    AppThemeM2 {
        WPJetpackIndividualPluginOverlayScreen(
            sites = listOf(
                SiteWithIndividualJetpackPlugins(
                    name = "Site 1",
                    url = "site1.wordpress.com",
                    individualPluginNames = listOf("Jetpack Social", "Jetpack Search")
                ),
                SiteWithIndividualJetpackPlugins(
                    name = "Site 2",
                    url = "site2.wordpress.com",
                    individualPluginNames = listOf("Jetpack Boost")
                ),
                SiteWithIndividualJetpackPlugins(
                    name = "Site 3",
                    url = "site3.wordpress.com",
                    individualPluginNames = listOf("Jetpack Social")
                ),
            ),
            onCloseClick = {},
            onPrimaryButtonClick = {},
            onSecondaryButtonClick = {},
        )
    }
}

