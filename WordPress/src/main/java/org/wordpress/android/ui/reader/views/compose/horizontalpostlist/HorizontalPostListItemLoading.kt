package org.wordpress.android.ui.reader.views.compose.horizontalpostlist

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.ui.compose.theme.AppColor
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun HorizontalPostListItemLoading() {
    val loadingColor = AppColor.Black.copy(
        alpha = 0.08F
    )
    Column(
        modifier = Modifier
            .width(240.dp)
            .height(340.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .aspectRatio(1f)
                    .background(loadingColor, shape = CircleShape),
            )
            Box(
                modifier = Modifier
                    .padding(start = Margin.Small.value)
                    .width(99.dp)
                    .height(8.dp)
                    .clip(shape = RoundedCornerShape(16.dp))
                    .background(loadingColor),
            )
        }
        Box(
            modifier = Modifier
                .padding(top = Margin.Large.value)
                .width(204.dp)
                .height(18.dp)
                .clip(shape = RoundedCornerShape(16.dp))
                .background(loadingColor),
        )
        Box(
            modifier = Modifier
                .padding(top = Margin.Large.value)
                .width(140.dp)
                .height(18.dp)
                .clip(shape = RoundedCornerShape(16.dp))
                .background(loadingColor),
        )
        Box(
            modifier = Modifier
                .padding(top = Margin.Large.value)
                .fillMaxWidth()
                .height(150.dp)
                .clip(shape = RoundedCornerShape(8.dp))
                .background(loadingColor),
        )
        Box(
            modifier = Modifier
                .padding(
                    start = Margin.Small.value,
                    top = Margin.Large.value,
                )
                .width(170.dp)
                .height(8.dp)
                .clip(shape = RoundedCornerShape(16.dp))
                .background(loadingColor),
        )
        Box(
            modifier = Modifier
                .padding(
                    start = Margin.Small.value,
                    top = Margin.Large.value,
                )
                .width(170.dp)
                .height(8.dp)
                .clip(shape = RoundedCornerShape(16.dp))
                .background(loadingColor),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun HorizontalPostListItemLoadingPreview() {
    AppTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            HorizontalPostListItemLoading()
        }
    }
}
