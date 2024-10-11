package org.wordpress.android.ui.main.jetpack.staticposter.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.components.buttons.PrimaryButton
import org.wordpress.android.ui.compose.components.buttons.SecondaryButton
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.theme.JpColorPalette
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.staticposter.UiData
import org.wordpress.android.ui.main.jetpack.staticposter.UiState
import org.wordpress.android.ui.main.jetpack.staticposter.toContentUiState
import org.wordpress.android.util.extensions.isRtl

@Composable
@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
fun JetpackStaticPoster(
    uiState: UiState.Content,
    onPrimaryClick: () -> Unit = {},
    onSecondaryClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
): Unit = with(uiState) {
    Scaffold(
        topBar = {
            if (showTopBar) {
                MainTopAppBar(
                    title = null,
                    navigationIcon = NavigationIcons.BackIcon,
                    onNavigationIconClick = onBackClick,
                )
            }
        },
    ) {
        val orientation = LocalConfiguration.current.orientation
        val verticalPadding = remember(orientation) {
            if (orientation == Configuration.ORIENTATION_LANDSCAPE) 60.dp else 30.dp
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 30.dp, vertical = verticalPadding)
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .fillMaxWidth(),
            ) {
                val animRes = if (LocalContext.current.isRtl()) R.raw.wp2jp_rtl else R.raw.wp2jp_left
                val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(animRes))
                LottieAnimation(lottieComposition)
                Text(
                    stringResource(
                        if (showPluralTitle)
                            R.string.wp_jp_static_poster_title_plural else R.string.wp_jp_static_poster_title,
                        uiStringText(featureName)
                    ),
                    style = MaterialTheme.typography.h1.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 48.sp
                    ),
                )
                Text(
                    stringResource(R.string.wp_jp_static_poster_message),
                    style = MaterialTheme.typography.body1.copy(fontSize = 17.sp),
                )
                Text(
                    stringResource(R.string.wp_jp_static_poster_footnote),
                    style = MaterialTheme.typography.body1.copy(colorResource(R.color.gray_50), 17.sp),
                )
            }
            PrimaryButton(
                stringResource(R.string.wp_jp_static_poster_button_primary),
                onPrimaryClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = JpColorPalette().primary,
                    contentColor = AppColor.White,
                ),
                padding = PaddingValues(bottom = 15.dp),
                textStyle = MaterialTheme.typography.body1.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
            SecondaryButton(
                stringResource(R.string.wp_jp_static_poster_button_secondary),
                onSecondaryClick,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Transparent,
                    contentColor = JpColorPalette().primary,
                ),
                padding = PaddingValues(0.dp),
                textStyle = MaterialTheme.typography.body1.copy(fontSize = 17.sp),
            ) {
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    painterResource(R.drawable.ic_external_v2),
                    stringResource(R.string.icon_desc),
                    tint = colorResource(R.color.jetpack_green_40)
                )
            }
        }
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
private fun PreviewJetpackStaticPoster() {
    AppTheme {
        Box {
            val uiState = UiData.STATS.toContentUiState()
            JetpackStaticPoster(uiState)
        }
    }
}
