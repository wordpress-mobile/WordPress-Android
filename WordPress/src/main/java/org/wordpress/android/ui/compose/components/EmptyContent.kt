package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

private fun Modifier.emptyContentTextModifier() = padding(horizontal = 30.dp).requiredWidthIn(max = 440.dp)

/**
 * Reusable component for empty screen states such as: empty lists, errors, and loading states. Based on the existing
 * [ActionableEmptyView] implementation, which is used throughout the project in the XML view screens.
 *
 * Note: this currently has a subset of the implementation of [ActionableEmptyView], see params below.
 *
 * @param modifier [Modifier] applied on the box that wraps the content of this Composable, the actual content is not
 * directly affected, so use this mainly to define the size and padding of the composable.
 * @param title (optional) [String] that will be displayed as title.
 * @param subtitle (optional) [String] that will be displayed as subtitle.
 * @param image (optional) Drawable resource ID for the image that will be displayed. Note the drawable original size
 * is directly used when displaying the image.
 * @param imageContentDescription (optional) Content Description passed directly to the internal [Image] Composable
 * showing the [image] drawable, for accessibility purposes.
 *
 * @see Modifier
 * @see Image
 */
@Composable
fun EmptyContent(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    @DrawableRes image: Int? = null,
    imageContentDescription: String? = null,
) {
    Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
    ) {
        Column(
                horizontalAlignment = Alignment.CenterHorizontally
        ) {
            image?.let { imageRes ->
                Image(
                        painterResource(imageRes),
                        contentDescription = imageContentDescription
                )
            }

            // show spacer if something will be shown below and something was shown above
            if (image != null && (title != null || subtitle != null)) {
                Spacer(Modifier.height(Margin.ExtraLarge.value))
            }

            // To match text color in ActionableEmptyView we need to provide the "medium" content alpha from Material
            CompositionLocalProvider(
                    LocalContentAlpha provides ContentAlpha.medium,
            ) {
                title?.let {
                    Text(
                            it,
                            modifier = Modifier.emptyContentTextModifier(),
                            style = MaterialTheme.typography.subtitle1.copy(
                                    fontSize = FontSize.ExtraLarge.value,
                            )
                    )
                }

                // show spacer if something will be shown below and title was shown above
                if (subtitle != null && title != null) {
                    Spacer(Modifier.height(Margin.Medium.value))
                }

                subtitle?.let {
                    Text(
                            it,
                            modifier = Modifier.emptyContentTextModifier(),
                            style = MaterialTheme.typography.subtitle1.copy(
                                    fontSize = FontSize.Large.value,
                            )
                    )
                }
            }
        }
    }
}

@Preview(name = "Everything", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentPreview() {
    AppTheme {
        EmptyContent(
                title = "Title",
                subtitle = "Subtitle",
                image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Image Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentImagePreview() {
    AppTheme {
        EmptyContent(
                image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Image and Title Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentImageTitlePreview() {
    AppTheme {
        EmptyContent(
                title = "Title",
                image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Image and Subtitle Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentImageSubtitlePreview() {
    AppTheme {
        EmptyContent(
                subtitle = "Subtitle",
                image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Title Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentTitlePreview() {
    AppTheme {
        EmptyContent(
                title = "Title",
        )
    }
}

@Preview(name = "Title and Subtitle Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentTitleSubtitlePreview() {
    AppTheme {
        EmptyContent(
                title = "Title",
                subtitle = "Subtitle",
        )
    }
}

@Preview(name = "Subtitle Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmptyContentSubtitlePreview() {
    AppTheme {
        EmptyContent(
                subtitle = "Subtitle",
        )
    }
}
