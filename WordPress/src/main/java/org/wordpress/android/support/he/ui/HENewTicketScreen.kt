package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import org.wordpress.android.R
import org.wordpress.android.support.common.model.UserInfo
import org.wordpress.android.support.he.model.AttachmentState
import org.wordpress.android.support.he.model.NewTicketFormState
import org.wordpress.android.support.he.util.AttachmentActionsListener
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
        includeAppLogs: Boolean,
            ) -> Unit,
    userInfo: UserInfo,
    isSendingNewConversation: Boolean = false,
    attachmentActionsListener: AttachmentActionsListener,
    formState: NewTicketFormState,
    onCategoryChange: (SupportCategory) -> Unit,
    onSubjectChange: (String) -> Unit,
    onSiteAddressChange: (String) -> Unit,
    onMessageTextChange: (String) -> Unit,
    onIncludeAppLogsChange: (Boolean) -> Unit,
) {
    HENewTicketScreenContent(
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick,
        onSubmit = onSubmit,
        userInfo = userInfo,
        isSendingNewConversation = isSendingNewConversation,
        attachmentState = formState.attachmentState,
        attachmentActionsListener = attachmentActionsListener,
        selectedCategory = formState.category,
        onCategoryChange = onCategoryChange,
        subject = formState.subject,
        onSubjectChange = onSubjectChange,
        siteAddress = formState.siteAddress,
        onSiteAddressChange = onSiteAddressChange,
        messageText = formState.message,
        onMessageTextChange = onMessageTextChange,
        includeAppLogs = formState.includeAppLogs,
        onIncludeAppLogsChange = onIncludeAppLogsChange,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HENewTicketScreenContent(
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit,
    onSubmit: (
        category: SupportCategory,
        subject: String,
        message: String,
        siteAddress: String,
        includeAppLogs: Boolean,
            ) -> Unit,
    userInfo: UserInfo,
    isSendingNewConversation: Boolean,
    attachmentState: AttachmentState,
    attachmentActionsListener: AttachmentActionsListener,
    selectedCategory: SupportCategory?,
    onCategoryChange: (SupportCategory) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    siteAddress: String,
    onSiteAddressChange: (String) -> Unit,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    includeAppLogs: Boolean,
    onIncludeAppLogsChange: (Boolean) -> Unit,
) {
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
                        onSubmit(category, subject, messageText, siteAddress, includeAppLogs)
                    }
                },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader(text = stringResource(R.string.he_support_need_help_with))

            SupportCategory.entries.forEach { category ->
                CategoryOption(
                    icon = category.icon,
                    label = stringResource(category.labelRes),
                    isSelected = selectedCategory == category,
                    onClick = { onCategoryChange(category) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(text = stringResource(R.string.he_support_issue_details))

            val subjectLabel = stringResource(R.string.he_support_subject_label)
            Text(
                text = subjectLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() }
            )

            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = subjectLabel },
                placeholder = {
                    Text(
                        text = stringResource(R.string.he_support_subject_placeholder)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            val siteAddressLabel = stringResource(R.string.he_support_site_address_label)
            Text(
                text = siteAddressLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .semantics { heading() }
            )

            OutlinedTextField(
                value = siteAddress,
                onValueChange = onSiteAddressChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = siteAddressLabel },
                placeholder = {
                    Text(
                        text = stringResource(R.string.he_support_site_address_placeholder)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Uri
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            TicketMainContentView(
                messageText = messageText,
                includeAppLogs = includeAppLogs,
                onMessageChanged = onMessageTextChange,
                onIncludeAppLogsChanged = onIncludeAppLogsChange,
                attachmentState = attachmentState,
                attachmentActionsListener = attachmentActionsListener
            )

            Spacer(modifier = Modifier.height(32.dp))

            SectionHeader(text = stringResource(R.string.he_support_contact_information))

            ContactInformationCard(
                userName = userInfo.userName,
                userEmail = userInfo.userEmail,
                userAvatarUrl = userInfo.avatarUrl
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
            .padding(bottom = 16.dp)
            .semantics { heading() }
    )
}

@Composable
private fun SendButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
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
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = stringResource(R.string.he_support_send_ticket_button),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.he_support_contact_email_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        if (userAvatarUrl.isNullOrEmpty()) {
                            Icon(
                                painter = painterResource(R.drawable.ic_user_white_24dp),
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            RemoteImage(
                                imageUrl = userAvatarUrl,
                                fallbackImageRes = R.drawable.ic_user_white_24dp,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.bodyMedium,
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = label },
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint =  if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                },
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            )

            RadioButton(
                selected = isSelected,
                onClick = null,
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun HENewTicketScreenPreviewContent(snackbarHostState: SnackbarHostState) {
    HENewTicketScreenContent(
        snackbarHostState = snackbarHostState,
        onBackClick = { },
        onSubmit = { _, _, _, _, _ -> },
        userInfo = UserInfo("Test user", "test.user@automattic.com", null),
        isSendingNewConversation = false,
        attachmentState = AttachmentState(),
        attachmentActionsListener = PreviewAttachmentActionsListener,
        selectedCategory = null,
        onCategoryChange = {},
        subject = "",
        onSubjectChange = {},
        siteAddress = "",
        onSiteAddressChange = {},
        messageText = "",
        onMessageTextChange = {},
        includeAppLogs = false,
        onIncludeAppLogsChange = {},
    )
}

private object PreviewAttachmentActionsListener : AttachmentActionsListener {
    override fun onAddImageClick() { /* Preview stub */ }
    override fun onRemoveImage(uri: Uri) { /* Preview stub */ }
}

@Preview(showBackground = true, name = "HE New Ticket Screen")
@Composable
private fun HENewTicketScreenPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = false) {
        HENewTicketScreenPreviewContent(snackbarHostState)
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewDark() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = true) {
        HENewTicketScreenPreviewContent(snackbarHostState)
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - WordPress")
@Composable
private fun HENewTicketScreenWordPressPreview() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HENewTicketScreenPreviewContent(snackbarHostState)
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewWordPressDark() {
    val snackbarHostState = remember { SnackbarHostState() }
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HENewTicketScreenPreviewContent(snackbarHostState)
    }
}
