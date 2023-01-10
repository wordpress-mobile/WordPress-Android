package org.wordpress.android.ui.accounts.login.components

import android.content.res.Configuration
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ParagraphStyle
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

private const val FIXED_FONT_SIZE = 40

@Composable
private fun LargeTexts() {
    val fontSize = (FIXED_FONT_SIZE / LocalDensity.current.fontScale).sp
    val lineHeight = fontSize * 1.05 // calculate line height to 5% larger than the font size

    val texts = stringArrayResource(R.array.login_prologue_revamped_jetpack_feature_texts)

    val secondaryColor = colorResource(R.color.text_color_jetpack_login_label_secondary)
    val primaryColor = colorResource(R.color.text_color_jetpack_login_label_primary)

    val styledText = buildAnnotatedString {
        texts.forEachIndexed { index, text ->
            withStyle(ParagraphStyle(lineHeight = lineHeight)) {
                when ((index + 1).isOdd) {
                    true -> withStyle(SpanStyle(color = secondaryColor)) {
                        append(text)
                    }
                    false -> withStyle(SpanStyle(color = primaryColor)) {
                        append(text)
                    }
                }
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

@Preview(showBackground = true, device = Devices.PIXEL_4)
@Preview(showBackground = true, device = Devices.PIXEL_4, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewLoopingText() {
    AppTheme {
        LoopingText()
    }
}
