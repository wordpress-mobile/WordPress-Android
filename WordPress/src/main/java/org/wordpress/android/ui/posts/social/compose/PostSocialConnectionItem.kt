package org.wordpress.android.ui.posts.social.compose

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.buttons.WPSwitch
import org.wordpress.android.ui.compose.theme.AppThemeM2
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.ui.posts.social.PostSocialConnection

@Composable
fun PostSocialConnectionItem(
    connection: PostSocialConnection,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onSharingChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(enabled) { onSharingChange(!connection.isSharingEnabled) }
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.MediumLarge.value)
    ) {
        val saturationMatrix = ColorMatrix().apply {
            setToSaturation(if (enabled) 1f else 0f)
        }
        AsyncImage(
            model = connection.iconResId,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.colorMatrix(saturationMatrix),
            alpha = if (enabled) 1f else ContentAlpha.disabled,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(Margin.ExtraLarge.value))
        Text(
            text = connection.externalName,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface
                .copy(alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled),
        )
        Spacer(modifier = Modifier.weight(1f))
        WPSwitch(
            checked = connection.isSharingEnabled,
            onCheckedChange = onSharingChange,
            enabled = enabled,
        )
    }
}

@Preview
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PostSocialConnectionItemPreview() {
    val connection = PostSocialConnection(
        connectionId = 0,
        service = "tumblr",
        label = "Tumblr",
        externalId = "myblog.tumblr.com",
        externalName = "My blog",
        iconResId = R.drawable.ic_social_tumblr,
        isSharingEnabled = true
    )
    var connectionState by remember { mutableStateOf(connection) }
    var disabledState by remember { mutableStateOf(connection.copy(isSharingEnabled = false)) }
    AppThemeM2 {
        Column {
            // enabled
            PostSocialConnectionItem(
                connection = connectionState,
                onSharingChange = { connectionState = connectionState.copy(isSharingEnabled = it) },
            )

            Divider()

            // disabled
            PostSocialConnectionItem(
                connection = disabledState,
                enabled = false,
                onSharingChange = { disabledState = disabledState.copy(isSharingEnabled = it) },
            )
        }
    }
}
