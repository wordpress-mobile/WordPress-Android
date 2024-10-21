package org.wordpress.android.ui.compose.components.text

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

@Composable
fun MessageM3(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        fontSize = 17.sp,
        color = colorResource(R.color.gray_50),
        modifier = modifier
            .padding(horizontal = 30.dp)
            .padding(top = 20.dp, bottom = 30.dp)
    )
}

@Preview
@Composable
private fun MessageM3Preview() {
    AppThemeM3 {
        MessageM3(text = "This message should be long enough so the preview wraps to more than one line")
    }
}
