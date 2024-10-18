package org.wordpress.android.ui.main.jetpack.staticposter.compose

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import org.wordpress.android.R
import org.wordpress.android.designsystem.heading1
import org.wordpress.android.ui.compose.components.buttons.PrimaryButtonM3
import org.wordpress.android.ui.compose.components.buttons.SecondaryButtonM3
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.JpColorPalette
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.staticposter.UiData
import org.wordpress.android.ui.main.jetpack.staticposter.UiState
import org.wordpress.android.ui.main.jetpack.staticposter.toContentUiState
import org.wordpress.android.util.extensions.isRtl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
fun JetpackStaticPoster(
    uiState: UiState.Content,
    onPrimaryClick: () -> Unit = {},
    onSecondaryClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
): Unit = with(uiState) {
    Scaffold(
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                stringResource(R.string.close)
                            )
                        }
                    },
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
                    style = MaterialTheme.typography.heading1.copy(
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 48.sp
                    ),
                )
                Text(
                    stringResource(R.string.wp_jp_static_poster_message),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                )
                Text(
                    stringResource(R.string.wp_jp_static_poster_footnote),
                    style = MaterialTheme.typography.bodyLarge.copy(colorResource(R.color.gray_50), 17.sp),
                )
            }
            PrimaryButtonM3(
                stringResource(R.string.wp_jp_static_poster_button_primary),
                onPrimaryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JpColorPalette().primary,
                    contentColor = AppColor.White,
                ),
                padding = PaddingValues(bottom = 15.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            )
            SecondaryButtonM3(
                stringResource(R.string.wp_jp_static_poster_button_secondary),
                onSecondaryClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = JpColorPalette().primary,
                ),
                padding = PaddingValues(0.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
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

@Preview(
    name = "Light Mode",
)
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewJetpackStaticPoster() {
    AppThemeM3 {
        val uiState = UiData.STATS.toContentUiState()
        JetpackStaticPoster(uiState)
    }
}
