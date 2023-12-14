package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.wordpress.android.ui.compose.theme.AppColor

@Composable
fun DynamicCardFeatureImage(imageUrl: String, modifier: Modifier = Modifier) {
    AsyncImage(
        model = imageUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        placeholder = ColorPainter(AppColor.Gray30),
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
            .clip(RoundedCornerShape(6.dp))
            .fillMaxWidth()
            .aspectRatio(2f)
    )
}
