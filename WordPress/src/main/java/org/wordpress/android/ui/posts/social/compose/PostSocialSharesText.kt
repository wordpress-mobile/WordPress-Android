package org.wordpress.android.ui.posts.social.compose

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.unit.Margin

@Composable
fun PostSocialSharesText(
    message: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Column(
        modifier = modifier
            .padding(horizontal = Margin.ExtraLarge.value, vertical = Margin.MediumLarge.value)
    ) {
        Text(
            text = message,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
                .copy(alpha = if (enabled) 0.74f else 0.38f),
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PostSocialSharesTextPreview() {
    val message = "27/30 Social shares remaining in the next 30 days"
    AppThemeM3 {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            PostSocialSharesText(
                message = message,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            PostSocialSharesText(
                message = message,
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
        }
    }
}
