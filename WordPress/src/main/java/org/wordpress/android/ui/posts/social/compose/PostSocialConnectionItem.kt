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
import androidx.compose.material.Switch
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
import org.wordpress.android.ui.compose.theme.AppTheme
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
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        val saturationMatrix = ColorMatrix().apply {
            setToSaturation(if (enabled) 1f else 0f)
        }
        AsyncImage(
            model = connection.iconUrl,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            colorFilter = ColorFilter.colorMatrix(saturationMatrix),
            alpha = if (enabled) 1f else ContentAlpha.disabled,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = connection.externalName,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onSurface
                .copy(alpha = if (enabled) ContentAlpha.high else ContentAlpha.disabled),
        )
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            enabled = enabled,
            checked = connection.isSharingEnabled,
            onCheckedChange = onSharingChange,
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
        iconUrl = "http://i.wordpress.com/wp-content/admin-plugins/publicize/assets/publicize-tumblr-2x.png",
        isSharingEnabled = true
    )
    var connectionState by remember { mutableStateOf(connection) }
    var disabledState by remember { mutableStateOf(connection) }
    AppTheme {
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
