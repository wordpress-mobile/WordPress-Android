package org.wordpress.android.ui.mysite.cards.dynamiccard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.styles.DashboardCardTypography

@Composable
fun DynamicCardHeader(
    title: String?,
    onHideMenuClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Title(title = title, modifier = Modifier.weight(1f))
        Menu(onHideMenuClicked)
    }
}

@Composable
private fun Title(title: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.padding(end = 16.dp),
        content = {
            title?.let { title ->
                Text(text = title, style = DashboardCardTypography.subTitle)
            }
        }
    )
}

@Composable
private fun Menu(onHideMenuClicked: () -> Unit) {
    IconButton(
        modifier = Modifier.size(32.dp),
        onClick = onHideMenuClicked
    ) {
        Icon(
            imageVector = Icons.Rounded.MoreVert,
            contentDescription = stringResource(id = R.string.more),
            tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        )
    }
}
