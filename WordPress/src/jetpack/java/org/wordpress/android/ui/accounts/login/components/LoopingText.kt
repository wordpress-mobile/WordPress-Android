package org.wordpress.android.ui.accounts.login.components

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.wordpress.android.R
import org.wordpress.android.ui.accounts.login.LocalPosition
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.util.extensions.isOdd

private val fontSize = 40.sp

@Composable
private fun LargeTexts() {
    val texts = stringArrayResource(R.array.login_prologue_revamped_jetpack_feature_texts)

    val oddColor = colorResource(R.color.text_color_jetpack_login_feature_odd)
    val evenColor = colorResource(R.color.text_color_jetpack_login_feature_even)

    val styledText = buildAnnotatedString {
        texts.forEachIndexed { index, text ->
            when ((index + 1).isOdd) {
                true -> withStyle(SpanStyle(color = oddColor)) {
                    append(text)
                }
                false -> withStyle(SpanStyle(color = evenColor)) {
                    append(text)
                }
            }
            if (index != texts.lastIndex) {
                append("\n")
            }
        }
    }

    Text(
            text = styledText,
            style = TextStyle(
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.03).sp,
            ),
    )
}

@Composable
fun LoopingText(modifier: Modifier = Modifier) {
    RepeatingColumn(
            position = LocalPosition.current,
            modifier = modifier
    ) {
        LargeTexts()
    }
}

@Preview(showBackground = true, device = Devices.PIXEL_4_XL)
@Composable
fun PreviewLoopingText() {
    AppTheme {
        LoopingText()
    }
}
