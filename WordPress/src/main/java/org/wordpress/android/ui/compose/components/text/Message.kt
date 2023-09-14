package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R

@Composable
fun Message(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 17.sp,
        style = TextStyle(letterSpacing = (-0.01).sp),
        color = colorResource(R.color.gray_50),
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 20.dp, bottom = 30.dp)
    )
}
