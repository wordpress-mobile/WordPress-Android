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
import androidx.compose.ui.semantics.clearAndSetSemantics
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

        SiteListScaffold(
                blurRadius = 4.dp,
                backgroundColor = colorResource(R.color.bg_jp_migration_buttons_panel),
                borderColor = colorResource(R.color.gray_10).copy(alpha = 0.5f),
                siteList = { clipModifier, buttonsHeightPx ->
                    SiteList(
                            uiState = uiState,
                            listState = listState,
                            bottomPaddingPx = buttonsHeightPx,
                            modifier = clipModifier,
                    )
                },
                blurBackground = { clipModifier, blurModifier, buttonsHeightPx ->
                    SiteList(
                            uiState = uiState,
                            listState = blurredListState,
                            userScrollEnabled = false,
                            bottomPaddingPx = buttonsHeightPx,
                            modifier = clipModifier.clearAndSetSemantics {},
                            blurModifier = blurModifier,
                    )
                },
                buttonsColumn = {
                    PrimaryButton(
                            text = uiStringText(primaryActionButton.text),
                            onClick = primaryActionButton.onClick,
                    )
                    SecondaryButton(
                            text = uiStringText(secondaryActionButton.text),
                            onClick = secondaryActionButton.onClick,
                    )
                }
        )
        LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
            blurredListState.scrollToItem(
                    listState.firstVisibleItemIndex,
                    listState.firstVisibleItemScrollOffset,
            )
        }
    }
}

private enum class SlotsEnum { SiteList, Buttons, ClippedBackground }

/**
 * This custom layout handles the positioning of the site list and the buttons container with the blurred background.
 * It also ensures the last items are visible when the list is scrolled all the way to the bottom by passing the
 * measured height of the buttons container to the site list avoiding unnecessary recomposition.
 */
@Composable
private fun SiteListScaffold(
    blurRadius: Dp,
    backgroundColor: Color,
    borderColor: Color,
    siteList: @Composable (clipModifier: Modifier, buttonsHeightPx: Int) -> Unit,
    blurBackground: @Composable (clipModifier: Modifier, blurModifier: Modifier, buttonsHeightPx: Int) -> Unit,
    buttonsColumn: @Composable () -> Unit,
) {
    SubcomposeLayout { constraints ->
        val buttonsPlaceables = subcompose(SlotsEnum.Buttons) {
            ColumnWithTopGlassBorder(
                    backgroundColor = backgroundColor,
                    borderColor = borderColor,
            ) {
                buttonsColumn()
            }
        }.map { it.measure(constraints) }

        val buttonsHeight = buttonsPlaceables[0].height

        val siteListClipShape = object : Shape {
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Rectangle {
                return Rectangle(
                        Rect(
                                top = 0f,
                                bottom = size.height - buttonsHeight,
                                left = 0f,
                                right = size.width,
                        )
                )
            }
        }

        val siteListPlaceables = subcompose(SlotsEnum.SiteList) {
            siteList(
                    clipModifier = Modifier.clip(siteListClipShape),
                    buttonsHeightPx = buttonsHeight
            )
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
            blurBackground(
                    clipModifier = Modifier.clip(buttonsClipShape),
                    blurModifier = Modifier.composed {
                        if (VERSION.SDK_INT >= VERSION_CODES.S) {
                            blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                        } else {
                            alpha(0.05f)
                        }
                    },
                    buttonsHeightPx = buttonsHeight,
            )
        }.map { it.measure(constraints) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            clippedBackgroundPlaceables.forEach { it.placeRelative(0, 0) }
            siteListPlaceables.forEach { it.placeRelative(0, 0) }
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
