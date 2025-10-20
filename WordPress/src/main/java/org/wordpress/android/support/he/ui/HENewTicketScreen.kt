package org.wordpress.android.support.he.ui

import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.components.MainTopAppBar
import org.wordpress.android.ui.compose.components.NavigationIcons
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.compose.theme.neutral

enum class SupportCategory(val icon: ImageVector, val labelRes: Int) {
    APPLICATION(Icons.Default.PhoneAndroid, R.string.he_support_category_application),
    JETPACK_CONNECTION(Icons.Default.Settings, R.string.he_support_category_jetpack_connection),
    SITE_MANAGEMENT(Icons.Default.Language, R.string.he_support_category_site_management),
    BILLING(Icons.Default.CreditCard, R.string.he_support_category_billing),
    TECHNICAL_ISSUES(Icons.Default.Settings, R.string.he_support_category_technical_issues),
    OTHER(Icons.AutoMirrored.Filled.Help, R.string.he_support_category_other)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HENewTicketScreen(
    onBackClick: () -> Unit,
    onSubmit: (category: SupportCategory, subject: String, siteAddress: String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf<SupportCategory?>(null) }
    var subject by remember { mutableStateOf("") }
    var siteAddress by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            MainTopAppBar(
                title = stringResource(R.string.he_support_contact_support_title),
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
                shape = RoundedCornerShape(12.dp)
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
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))
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
        Box(
            modifier = Modifier
                .size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

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
    AppThemeM3(isDarkTheme = false) {
        HENewTicketScreen(
            onBackClick = { },
            onSubmit = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewDark() {
    AppThemeM3(isDarkTheme = true) {
        HENewTicketScreen(
            onBackClick = { },
            onSubmit = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - WordPress")
@Composable
private fun HENewTicketScreenWordPressPreview() {
    AppThemeM3(isDarkTheme = false, isJetpackApp = false) {
        HENewTicketScreen(
            onBackClick = { },
            onSubmit = { _, _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "HE New Ticket Screen - Dark WordPress", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun HENewTicketScreenPreviewWordPressDark() {
    AppThemeM3(isDarkTheme = true, isJetpackApp = false) {
        HENewTicketScreen(
            onBackClick = { },
            onSubmit = { _, _, _ -> }
        )
    }
}
