package org.wordpress.android.ui.bloggingprompts.promptslist.compose

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import org.wordpress.android.ui.bloggingprompts.promptslist.model.BloggingPromptsListItemModel
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.compose.unit.FontSize
import org.wordpress.android.ui.compose.unit.Margin
import org.wordpress.android.util.LocaleManager
import java.text.SimpleDateFormat

private const val MONTH_DAY_FORMAT = "MMM d"
private val answeredColor = Color(0xFF008A20)

private val ItemSubtitleTextStyle
    @ReadOnlyComposable
    @Composable
    get() = TextStyle(
            color = LocalContentColor.current.copy(alpha = 0.6f),
            fontSize = FontSize.Small.value,
            fontWeight = FontWeight.W400,
            letterSpacing = 0.4.sp,
    )

@Composable
fun BloggingPromptsListItem(
    model: BloggingPromptsListItemModel,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormat = remember(context) {
        SimpleDateFormat(MONTH_DAY_FORMAT, LocaleManager.getSafeLocale(context))
    }

    Column(
            modifier = modifier
                    .background(MaterialTheme.colors.surface)
                    .padding(Margin.ExtraLarge.value),
            verticalArrangement = Arrangement.spacedBy(Margin.Small.value),
    ) {
        Text(
                text = model.text,
                style = TextStyle(
                        fontSize = FontSize.Large.value,
                        fontWeight = FontWeight.W700,
                        fontFamily = FontFamily.Serif,
                ),
        )
        Row {
            Text(
                    text = dateFormat.format(model.date),
                    style = ItemSubtitleTextStyle,
            )
            ItemSubtitleDivider()
            Text(
                    text = "${model.answersCount} answers",
                    style = ItemSubtitleTextStyle,
            )

            if (model.isAnswered) {
                ItemSubtitleDivider()
                Text(
                        text = "✓ Answered",
                        color = answeredColor,
                        style = ItemSubtitleTextStyle,
                )
            }
        }
    }
}

@Composable
private fun ItemSubtitleDivider() {
    Text(
            text = "•",
            modifier = Modifier.padding(horizontal = Margin.Medium.value),
            style = ItemSubtitleTextStyle,
    )
}

@Preview(widthDp = 360)
@Preview(widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun BloggingPromptsListItemPreview(
    @PreviewParameter(BloggingPromptsListItemPreviewProvider::class) model: BloggingPromptsListItemModel
) {
    AppTheme {
        BloggingPromptsListItem(model)
    }
}
