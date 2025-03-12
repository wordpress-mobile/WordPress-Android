package org.wordpress.android.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

/**
 * Banner for Me screen when user's email hasn't yet been verified
 */
@Composable
fun MeEmailUnverifiedBanner(
    emailAddress: String,
    onSendLinkClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mail_white_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.me_email_verification_verify_email),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            text = stringResource(R.string.me_email_verification_verify_email_description, emailAddress),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = stringResource(R.string.me_email_verification_verify_email_send_link),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.jetpack_green_50),
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    onSendLinkClick()
                }
        )
    }
}

/**
 * Banner for Me screen when user's email hasn't yet been verified but a verification link has been requested
 */
@Composable
fun MeEmailVerificationRequestedBanner(
    emailAddress: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gridicons_checkmark_circle),
                contentDescription = null,
                tint = colorResource(R.color.jetpack_green_50),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.me_email_verification_sent),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.jetpack_green_50)
            )
        }

        Text(
            text = stringResource(R.string.me_email_verification_sent_description, emailAddress),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Preview
@Composable
fun MeEmailUnverifiedPreview() {
    MeEmailUnverifiedBanner(
        emailAddress = "vonnegut@example.com",
        onSendLinkClick = {}
    )
}

@Preview
@Composable
fun MeEmailVerificationRequestedPreview() {
    MeEmailVerificationRequestedBanner(
        emailAddress = "vonnegut@example.com",
    )
}
