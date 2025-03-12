package org.wordpress.android.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R

/**
 * Banner for Me screen when user's email hasn't yet been verified
 *
 * @param isUnverified True if email needs to be verified, False if it's awaiting verification (ie: link sent)
 * @param emailAddress The email address to display for the user
 */
@Composable
fun MeEmailVerificationBanner(
    isUnverified: Boolean,
    emailAddress: String,
    onSendLinkClick: () -> Unit,
) {
    val title: String
    val description: String
    val titleColor: Color
    val iconId: Int
    val iconTint: Color
    if (isUnverified) {
        title = stringResource(R.string.me_email_verification_verify_email)
        description = stringResource(R.string.me_email_verification_verify_email_description, emailAddress)
        titleColor = MaterialTheme.colorScheme.onSurface
        iconId = R.drawable.ic_mail_white_24dp
        iconTint = MaterialTheme.colorScheme.onSurface
    } else {
        title = stringResource(R.string.me_email_verification_sent)
        description = stringResource(R.string.me_email_verification_sent_description, emailAddress)
        titleColor = colorResource(R.color.jetpack_green_50)
        iconId = R.drawable.ic_gridicons_checkmark_circle
        iconTint = titleColor
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
                painter = painterResource(iconId),
                contentDescription = null,
                tint = iconTint,
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

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge
        )

        if (isUnverified) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.me_email_verification_verify_email_send_link),
                style = MaterialTheme.typography.bodyLarge,
                color = colorResource(R.color.jetpack_green_50),
                modifier = Modifier.clickable {
                    onSendLinkClick()
                }
            )
        }
    }
}

@Preview
@Composable
fun MeEmailUnverifiedPreview() {
    MeEmailVerificationBanner(
        isUnverified = true,
        emailAddress = "vonnegut@example.com",
        onSendLinkClick = {}
    )
}

@Preview
@Composable
fun MeEmailVerifyingPreview() {
    MeEmailVerificationBanner(
        isUnverified = false,
        emailAddress = "vonnegut@example.com",
        onSendLinkClick = {}
    )
}
