package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.dataview.compose.RemoteImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HENewTicketScreen(
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSubmit: (
        category: SupportCategory,
        subject: String,
        messageText: String,
        siteAddress: String,
            ) -> Unit,
    userName: String = "",
    userEmail: String = "",
    userAvatarUrl: String? = null,
    isSendingNewConversation: Boolean = false
) {
    var selectedCategory by remember { mutableStateOf<SupportCategory?>(null) }
    var subject by remember { mutableStateOf("") }
    var siteAddress by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var includeAppLogs by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.he_support_contact_support_title),
                navigationIcon = NavigationIcons.BackIcon,
                onNavigationIconClick = onBackClick
            )
        },
        bottomBar = {
            SendButton(
                enabled = selectedCategory != null && subject.isNotBlank() && messageText.isNotBlank(),
                isLoading = isSendingNewConversation,
                onClick = {
                    selectedCategory?.let { category ->
                        onSubmit(category, subject, messageText, siteAddress)
                    }
                }
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
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.he_support_need_help_with),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            SupportCategory.entries.forEach { category ->
                CategoryOption(
                    icon = category.icon,
                    label = stringResource(category.labelRes),
                    isSelected = selectedCategory == category,
                    onClick = { selectedCategory = category }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.he_support_issue_details),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.he_support_subject_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.he_support_subject_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.he_support_site_address_label),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = siteAddress,
                onValueChange = { siteAddress = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.he_support_site_address_placeholder),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            Spacer(modifier = Modifier.height(32.dp))

            TicketMainContentView(
                messageText = messageText,
                includeAppLogs = includeAppLogs,
                onMessageChanged = { message -> messageText = message },
                onIncludeAppLogsChanged = { checked -> includeAppLogs = checked },
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.he_support_contact_information),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ContactInformationCard(
                userName = userName,
                userEmail = userEmail,
                userAvatarUrl = userAvatarUrl
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SendButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .padding(16.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.he_support_send_ticket_button),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
private fun ContactInformationCard(
    userName: String,
    userEmail: String,
    userAvatarUrl: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.he_support_contact_email_message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    if (userAvatarUrl.isNullOrEmpty()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_user_white_24dp),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        RemoteImage(
                            imageUrl = userAvatarUrl,
                            fallbackImageRes = R.drawable.ic_user_white_24dp,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        )

        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen")
@Composable
private fun HENewTicketScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = false) {
        HENewTicketScreen(
            snackbarHostState = snackbarHostState,
            onBackClick = { },
            onSubmit = { _, _, _, _ -> },
            userName = "Test user",
            userEmail = "test.user@automattic.com",
            userAvatarUrl = null
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewDark() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = true) {
        HENewTicketScreen(
            snackbarHostState = snackbarHostState,
            onBackClick = { },
            onSubmit = { _, _, _, _ -> },
            userName = "Test user",
            userEmail = "test.user@automattic.com",
            userAvatarUrl = null
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - WordPress")
@Composable
private fun HENewTicketScreenWordPressPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HENewTicketScreen(
            snackbarHostState = snackbarHostState,
            onBackClick = { },
            onSubmit = { _, _, _, _ -> },
            userName = "Test user",
            userEmail = "test.user@automattic.com",
            userAvatarUrl = null
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewWordPressDark() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HENewTicketScreen(
            snackbarHostState = snackbarHostState,
            onBackClick = { },
            onSubmit = { _, _, _, _ -> },
            userName = "Test user",
            userEmail = "test.user@automattic.com",
            userAvatarUrl = null
        )
    }
}
