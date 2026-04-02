package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.coreui.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin

private fun Modifier.emptyContentTextModifier() = padding(horizontal = 30.dp)

/**
 * Reusable Material 3 component for empty screen states
 *
 * @param modifier [Modifier] applied on the containing [Box]
 * @param title [String] that will be displayed as title
 * @param subtitle [String?] that will be displayed as subtitle
 * @param image [Int?] Drawable resource ID for the image that will be displayed
 * @param imageContentDescription [String?] Content Description for the above image
 */
@Composable
fun EmptyContentM3(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    @DrawableRes image: Int? = null,
    imageContentDescription: String? = null,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            image?.let { imageRes ->
                Image(
                    painterResource(imageRes),
                    contentDescription = imageContentDescription
                )
                Spacer(Modifier.height(Margin.ExtraLarge.value))
            }

            Text(
                title,
                modifier = Modifier.emptyContentTextModifier(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = FontSize.ExtraLarge.value,
                    fontWeight = FontWeight.Normal
                )
            )

            subtitle?.let {
                Spacer(Modifier.height(Margin.Medium.value))
                Text(
                    it,
                    modifier = Modifier.emptyContentTextModifier(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = FontSize.Large.value,
                        fontWeight = FontWeight.Normal
                    )
                )
            }
        }
    }
}

@Preview(name = "Everything", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyContentM3Preview() {
    AppThemeM3 {
        EmptyContentM3(
            title = "Title",
            subtitle = "Subtitle",
            image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Image and Title Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyContentM3ImageTitlePreview() {
    AppThemeM3 {
        EmptyContentM3(
            title = "Title",
            image = R.drawable.img_illustration_empty_results_216dp,
        )
    }
}

@Preview(name = "Title Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyContentM3TitlePreview() {
    AppThemeM3 {
        EmptyContentM3(
            title = "Title",
        )
    }
}

@Preview(name = "Title and Subtitle Only", showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun EmptyContentM3TitleSubtitlePreview() {
    AppThemeM3 {
        EmptyContentM3(
            title = "Title",
            subtitle = "Subtitle",
        )
    }
}
