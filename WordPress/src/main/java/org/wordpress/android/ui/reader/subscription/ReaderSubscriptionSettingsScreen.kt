package org.wordpress.android.ui.reader.subscription

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3

private val HANDLE_HEIGHT = 4.dp
private val HANDLE_WIDTH = 32.dp
private val HANDLE_MARGIN_TOP = 4.dp
private val HORIZONTAL_PADDING = 16.dp
private val BOTTOM_PADDING = 20.dp
private val TITLE_MARGIN_TOP = 20.dp
private val BLOG_NAME_MARGIN_TOP = 4.dp
private val DIVIDER_MARGIN_TOP = 12.dp
private val SWITCH_VERTICAL_PADDING = 12.dp
private const val DISABLED_ALPHA = 0.38f

@Composable
fun ReaderSubscriptionSettingsScreen(
    uiState: ReaderSubscriptionSettingsUiState,
    onNotifyPostsToggled: (Boolean) -> Unit,
    onEmailPostsToggled: (Boolean) -> Unit,
    onEmailCommentsToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HORIZONTAL_PADDING)
                .padding(bottom = BOTTOM_PADDING)
        ) {
            // Handle
            BottomSheetHandle()

            // Title
            Text(
                text = stringResource(R.string.reader_subscription_settings_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = TITLE_MARGIN_TOP)
            )

            // Blog name/URL
            Text(
                text = uiState.blogUrl.ifEmpty { uiState.blogName },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = BLOG_NAME_MARGIN_TOP)
            )

            Spacer(modifier = Modifier.height(DIVIDER_MARGIN_TOP))
            HorizontalDivider()

            // Notify me of new posts
            SubscriptionSettingsSwitch(
                text = stringResource(R.string.reader_subscription_settings_notify_posts),
                checked = uiState.notifyPostsEnabled,
                onCheckedChange = onNotifyPostsToggled,
                enabled = !uiState.isLoading
            )

            HorizontalDivider()

            // Email me new posts
            SubscriptionSettingsSwitch(
                text = stringResource(R.string.reader_subscription_settings_email_posts),
                checked = uiState.emailPostsEnabled,
                onCheckedChange = onEmailPostsToggled,
                enabled = !uiState.isLoading
            )

            HorizontalDivider()

            // Email me new comments
            SubscriptionSettingsSwitch(
                text = stringResource(R.string.reader_subscription_settings_email_comments),
                checked = uiState.emailCommentsEnabled,
                onCheckedChange = onEmailCommentsToggled,
                enabled = !uiState.isLoading
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun BottomSheetHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = HANDLE_MARGIN_TOP),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = HANDLE_WIDTH, height = HANDLE_HEIGHT)
                .alpha(DISABLED_ALPHA)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(HANDLE_HEIGHT / 2)
                )
        )
    }
}

@Composable
private fun SubscriptionSettingsSwitch(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = SWITCH_VERTICAL_PADDING),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ReaderSubscriptionSettingsScreenPreview() {
    AppThemeM3 {
        ReaderSubscriptionSettingsScreen(
            uiState = ReaderSubscriptionSettingsUiState(
                blogId = 123L,
                blogName = "Example Blog",
                blogUrl = "example.wordpress.com",
                notifyPostsEnabled = true,
                emailPostsEnabled = false,
                emailCommentsEnabled = true
            ),
            onNotifyPostsToggled = {},
            onEmailPostsToggled = {},
            onEmailCommentsToggled = {}
        )
    }
}

@Preview(name = "Loading State")
@Composable
private fun ReaderSubscriptionSettingsScreenLoadingPreview() {
    AppThemeM3 {
        ReaderSubscriptionSettingsScreen(
            uiState = ReaderSubscriptionSettingsUiState(
                blogId = 123L,
                blogName = "Example Blog",
                blogUrl = "example.wordpress.com",
                isLoading = true,
                notifyPostsEnabled = true,
                emailPostsEnabled = false,
                emailCommentsEnabled = true
            ),
            onNotifyPostsToggled = {},
            onEmailPostsToggled = {},
            onEmailCommentsToggled = {}
        )
    }
}
