package org.wordpress.android.ui.main.jetpack.migration.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import org.wordpress.android.R.color
import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.compose.unit.Margin.MediumLarge

@Composable
fun BoxScope.UserAvatarImage(
    avatarUrl: String,
    modifier: Modifier = Modifier,
) {
    val painter = rememberImagePainter(avatarUrl) {
        placeholder(drawable.bg_rectangle_placeholder_globe_32dp)
        error(drawable.bg_rectangle_placeholder_globe_32dp)
        crossfade(true)
    }
    Image(
            painter = painter,
            contentDescription = stringResource(string.jp_migration_avatar_content_description),
            modifier = modifier
                    .align(Alignment.TopEnd)
                    .padding(top = MediumLarge.value, end = 30.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(1.dp, colorResource(color.black_translucent_20), CircleShape)
                    .background(MaterialTheme.colors.surface)
    )
}
