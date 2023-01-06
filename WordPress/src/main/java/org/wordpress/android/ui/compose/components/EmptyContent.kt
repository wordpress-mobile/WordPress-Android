package org.wordpress.android.ui.compose.components

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.R
import org.wordpress.android.ui.ActionableEmptyView
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

/**
 * Reusable component for empty screen states such as: empty lists, errors, and loading states. Based on the existing
 * [ActionableEmptyView] implementation, which is used throughout the project in the XML view screens.
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

            // show spacer if something will be shown below
            if (title != null || subtitle != null) {
                Spacer(Modifier.height(Margin.ExtraLarge.value))
            }

            title?.let {
                Text(
                        it,
                        style = MaterialTheme.typography.subtitle1 // TODO thomashorta set the correct style
                )
            }
            subtitle?.let {
                // if there's a subtitle, add a spacer before it
                Spacer(Modifier.height(Margin.Medium.value))

                Text(
                        it,
                        color = LocalContentColor.current.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.subtitle2 // TODO thomashorta set the correct style
                )
            }
        }
    }
}

@Preview(showBackground = true)
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
