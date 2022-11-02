package org.wordpress.android.ui.main.jetpack.migration.components

import android.content.res.Configuration
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline.Rectangle
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.ColumnWithTopGlassBorder
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.utils.uiStringText
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomePrimaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.ActionButton.WelcomeSecondaryButton
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.SiteListItemUiState
import org.wordpress.android.ui.main.jetpack.migration.JetpackMigrationViewModel.StepUiState

@Composable
fun WelcomeStep(uiState: StepUiState.Welcome) = with(uiState) {
    Box {
        val listState = rememberLazyListState()
        val blurredListState = rememberLazyListState()

        SiteListLayout(
                blurRadius = 4.dp,
                backgroundColor = colorResource(R.color.bg_jp_migration_buttons_panel),
                borderColor = colorResource(R.color.gray_10).copy(alpha = 0.5f),
                siteList = { buttonsHeightPx ->
                    SiteList(
                            uiState = uiState,
                            listState = listState,
                            bottomPaddingPx = buttonsHeightPx,
                    )
                },
                background = { clipModifier, blurModifier, buttonsHeightPx ->
                    SiteList(
                            uiState = uiState,
                            listState = blurredListState,
                            userScrollEnabled = false,
                            bottomPaddingPx = buttonsHeightPx,
                            modifier = clipModifier,
                            blurModifier = blurModifier,
                    )
                },
        ) {
            PrimaryButton(
                    text = uiStringText(primaryActionButton.text),
                    onClick = primaryActionButton.onClick,
            )
            SecondaryButton(
                    text = uiStringText(secondaryActionButton.text),
                    onClick = secondaryActionButton.onClick,
            )
        }
        LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            blurredListState.scrollToItem(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset,
            )
        }
    }
}

private enum class SlotsEnum { SiteList, Buttons, ClippedBackground }

@Composable
private fun SiteListLayout(
    blurRadius: Dp,
    backgroundColor: Color,
    borderColor: Color,
    background: @Composable (clipModifier: Modifier, blurModifier: Modifier, buttonsHeightPx: Int) -> Unit,
    siteList: @Composable (buttonsHeightPx: Int) -> Unit,
    content: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val buttonsPlaceables = subcompose(SlotsEnum.Buttons) {
            ColumnWithTopGlassBorder(
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
            ) {
                content()
            }
        }.map { it.measure(constraints) }

        val buttonsHeight = buttonsPlaceables[0].height

        val siteListPlaceables = subcompose(SlotsEnum.SiteList) {
            siteList(buttonsHeightPx = buttonsHeight)
        }.map { it.measure(constraints) }

        val buttonsClipShape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Rectangle {
                return Rectangle(
                        Rect(
                                bottom = size.height,
                                left = 0f,
                                right = size.width,
                                top = size.height - buttonsHeight,
                        )
                )
            }
        }

        val clippedBackgroundPlaceables = subcompose(SlotsEnum.ClippedBackground) {
            background(
                    clipModifier = Modifier.clip(buttonsClipShape),
                    blurModifier = Modifier.composed {
                        if (VERSION.SDK_INT >= VERSION_CODES.S) {
                            blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                        } else {
                            // On versions older than Android 12 the blur modifier is not supported,
                            // so we make the text transparent to have the buttons stand out.
                            alpha(0.05f)
                        }
                    },
                    buttonsHeightPx = buttonsHeight,
            )
        }.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            siteListPlaceables.forEach { it.placeRelative(0, 0) }
            clippedBackgroundPlaceables.forEach { it.placeRelative(0, 0) }
            buttonsPlaceables.forEach { it.placeRelative(0, constraints.maxHeight - buttonsHeight) }
        }
    }
}

val previewSiteListItems = mutableListOf<SiteListItemUiState>().apply {
    repeat(10) {
        add(
                SiteListItemUiState(
                        id = it.toLong(),
                        name = "Site $it",
                        url = "site-$it-name.com",
                        iconUrl = "",
                )
        )
    }
}

@Preview(showBackground = true, widthDp = 414, heightDp = 897)
@Preview(showBackground = true, widthDp = 414, heightDp = 897, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewContentState() {
    val uiState = StepUiState.Welcome(
            previewSiteListItems,
            primaryActionButton = WelcomePrimaryButton {},
            secondaryActionButton = WelcomeSecondaryButton {},
    )
    AppTheme {
        Box {
            WelcomeStep(uiState)
            UserAvatarImage(avatarUrl = "")
        }
    }
}
