package org.wordpress.android.ui.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

/**
 * Email verification banner for Me screen
 * @param isUnverified Whether the email is unverified, when false it means it's been verified
 * @param emailAddress The email address to display for the user
 */
@Composable
fun MeEmailVerificationBanner(
    isUnverified: Boolean,
    emailAddress: String,
) {
    val title = if (isUnverified) {
        stringResource(R.string.me_email_verification_verify_email)
    } else {
        stringResource(R.string.me_email_verification_sent,)
    }
    val titleColor = if (isUnverified) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.onTertiary
    }
    val description = if (isUnverified) {
        stringResource(R.string.me_email_verification_verify_email_description, emailAddress)
    } else {
        stringResource(R.string.me_email_verification_sent_description, emailAddress)
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = titleColor
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = description, style = MaterialTheme.typography.bodyMedium)

        if (isUnverified) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.me_email_verification_verify_email_send_link),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Preview
@Composable
fun MeGravatarQuickEditorUnverifiedPreview() {
    MeEmailVerificationBanner(isUnverified = true, "kurt.vonnegut@example.com")
}

@Preview
@Composable
fun MeGravatarQuickEditorVerifyingPreview() {
    MeEmailVerificationBanner(isUnverified = false, "kurt.vonnegut@example.com")
}
