package org.wordpress.android.support.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    userName: String,
    userEmail: String,
    userAvatarUrl: String?,
    isLoggedIn: Boolean,
    showAskTheBots: Boolean,
    showAskHappinessEngineers: Boolean,
    versionName: String,
    onBackClick: () -> Unit,
    onLoginClick: () -> Unit,
    onHelpCenterClick: () -> Unit,
    onAskTheBotsClick: () -> Unit,
    onAskHappinessEngineersClick: () -> Unit,
    onApplicationLogsClick: () -> Unit,
) {
    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.support_screen_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // User Profile or Login Button
            if (isLoggedIn) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUrl.isNullOrEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_user_white_24dp),
                                contentDescription = stringResource(
                                    R.string.support_screen_user_avatar_content_description
                                ),
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            RemoteImage(
                                imageUrl = userAvatarUrl,
                                fallbackImageRes = R.drawable.ic_user_white_24dp,
                                modifier = Modifier.size(64.dp)
                                    .clip(CircleShape),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val loginButtonText = stringResource(R.string.support_screen_login_button)
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .semantics { contentDescription = loginButtonText }
                ) {
                    Text(text = loginButtonText)
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // How can we help? Section
            SectionHeader(
                title = stringResource(R.string.support_screen_how_can_we_help_title)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SupportOptionItem(
                title = stringResource(R.string.support_screen_help_center_title),
                description = stringResource(R.string.support_screen_help_center_description),
                onClick = onHelpCenterClick
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            if (showAskTheBots) {
                SupportOptionItem(
                    title = stringResource(R.string.support_screen_ask_bots_title),
                    description = stringResource(R.string.support_screen_ask_bots_description),
                    onClick = onAskTheBotsClick
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            if (showAskHappinessEngineers) {
                SupportOptionItem(
                    title = stringResource(R.string.support_screen_ask_happiness_engineers_title),
                    description = stringResource(R.string.support_screen_ask_happiness_engineers_description),
                    onClick = onAskHappinessEngineersClick,
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            }

            // Diagnostics Section
            SectionHeader(
                title = stringResource(R.string.support_screen_diagnostics_section_title)
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            SupportOptionItem(
                title = stringResource(R.string.support_screen_application_logs_title),
                description = stringResource(R.string.support_screen_application_logs_description),
                onClick = onApplicationLogsClick,
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Version Name
            Text(
                text = stringResource(R.string.version_with_name_param, versionName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 24.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SupportOptionItem(
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$title. $description"
            }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Support Screen - Light - Logged In")
@Composable
private fun SupportScreenPreview() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = true) {
        SupportScreen(
            userName = "Test user",
            userEmail = "test.user@gmail.com",
            userAvatarUrl = null,
            isLoggedIn = true,
            showAskTheBots = true,
            showAskHappinessEngineers = true,
            versionName = "1.0.0",
            onBackClick = {},
            onLoginClick = {},
            onHelpCenterClick = {},
            onAskTheBotsClick = {},
            onAskHappinessEngineersClick = {},
            onApplicationLogsClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Support Screen - Dark - Logged In", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SupportScreenPreviewDark() {
    AppThemeM3(isDarkTheme = true, isJetpackApp = true) {
        SupportScreen(
            userName = "Test user",
            userEmail = "test.user@gmail.com",
            userAvatarUrl = null,
            isLoggedIn = true,
            showAskTheBots = true,
            showAskHappinessEngineers = true,
            versionName = "1.0.0",
            onBackClick = {},
            onLoginClick = {},
            onHelpCenterClick = {},
            onAskTheBotsClick = {},
            onAskHappinessEngineersClick = {},
            onApplicationLogsClick = {}
        )
    }
}

@Preview(showBackground = true, name = "Support Screen - Logged Out")
@Composable
private fun SupportScreenPreviewLoggedOut() {
    AppThemeM3(isDarkTheme = false) {
        SupportScreen(
            userName = "",
            userEmail = "",
            userAvatarUrl = null,
            isLoggedIn = false,
            showAskTheBots = false,
            showAskHappinessEngineers = false,
            versionName = "1.0.0",
            onBackClick = {},
            onLoginClick = {},
            onHelpCenterClick = {},
            onAskTheBotsClick = {},
            onAskHappinessEngineersClick = {},
            onApplicationLogsClick = {}
        )
    }
}
