package org.wordpress.android.ui.main.jetpack.migration.compose.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.R.dimen

@Composable
fun ScreenIcon(
    @DrawableRes iconRes: Int,
    modifier: Modifier = Modifier,
) {
    Image(
            painter = painterResource(iconRes),
            contentDescription = stringResource(R.string.icon_desc),
            modifier = modifier
                    .padding(horizontal = dimensionResource(dimen.jp_migration_padding_horizontal))
                    .padding(top = 34.dp)
                    .width(123.dp)
                    .height(65.dp)
    )
}
