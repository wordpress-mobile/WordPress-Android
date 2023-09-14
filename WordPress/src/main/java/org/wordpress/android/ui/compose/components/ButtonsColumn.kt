package org.wordpress.android.ui.compose.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

@Composable
fun ButtonsColumn(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.padding(bottom = 10.dp)
    ) {
        Divider(
            color = colorResource(R.color.gray_10),
            thickness = 0.5.dp,
        )
        content()
    }
}
