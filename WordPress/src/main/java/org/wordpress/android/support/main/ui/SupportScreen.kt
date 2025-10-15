package org.wordpress.android.support.main.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
    hasAccessToken: Boolean,
    showAskTheBots: Boolean,
    showAskHappinessEngineers: Boolean,
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
                title = "Support",
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
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Support Profile Section
            Text(
                text = "Support Profile",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // User Profile Card or Login Button
            if (hasAccessToken) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar placeholder
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
                                contentDescription = "User avatar",
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
                Button(
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Log into WordPress.com")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // How can we help? Section
            Text(
                text = "How can we help?",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Support Options Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column {
                    SupportOptionItem(
                        title = "Help Center",
                        description = "Documentation and Tutorials to help you get started",
                        onClick = onHelpCenterClick
                    )

                    if (showAskTheBots) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        SupportOptionItem(
                            title = "Ask the bots",
                            description = "Get quick answers to common questions",
                            onClick = onAskTheBotsClick
                        )
                    }

                    if (showAskHappinessEngineers) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        )

                        SupportOptionItem(
                            title = "Ask the Happiness Engineers",
                            description = "For your tough questions. We'll reply via email",
                            onClick = onAskHappinessEngineersClick,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Diagnostics Section
            Text(
                text = "Diagnostics",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Application Logs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                SupportOptionItem(
                    title = "Application Logs",
                    description = "Advanced tool to debug issues",
                    onClick = onApplicationLogsClick,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
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
            .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, name = "Support Screen - Light - Logged In")
@Composable
private fun SupportScreenPreview() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        SupportScreen(
            userName = "Test user",
            userEmail = "test.user@gmail.com",
            userAvatarUrl = null,
            hasAccessToken = true,
            showAskTheBots = true,
            showAskHappinessEngineers = true,
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
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        SupportScreen(
            userName = "Test user",
            userEmail = "test.user@gmail.com",
            userAvatarUrl = null,
            hasAccessToken = true,
            showAskTheBots = true,
            showAskHappinessEngineers = true,
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
            hasAccessToken = false,
            showAskTheBots = false,
            showAskHappinessEngineers = false,
            onBackClick = {},
            onLoginClick = {},
            onHelpCenterClick = {},
            onAskTheBotsClick = {},
            onAskHappinessEngineersClick = {},
            onApplicationLogsClick = {}
        )
    }
}
