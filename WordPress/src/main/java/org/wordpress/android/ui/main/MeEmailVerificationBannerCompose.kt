package org.wordpress.android.ui.main

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import org.wordpress.android.ui.compose.theme.AppThemeM3

/**
 * Banner when user's email hasn't yet been verified
 */
@Composable
fun MeEmailUnverifiedBanner(
    emailAddress: String,
    onSendLinkClick: () -> Unit,
) {
    MeEmailVerificationContainer {
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
            text = stringResource(
                R.string.me_email_verification_verify_email_description,
                emailAddress
            ),
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
 * Banner when user's email hasn't been verified but a verification link has been requested
 */
@Composable
fun MeEmailVerificationSendingBanner(
    emailAddress: String,
) {
    MeEmailVerificationContainer {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(24.dp)
            )
            Text(
                text = stringResource(R.string.me_email_verification_sending),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 16.dp)
            )
        }

        Row {
            Text(
                text = stringResource(
                    R.string.me_email_verification_sending_description,
                    emailAddress
                ),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * Banner when user's email hasn't yet been verified but a verification link has been sent
 */
@Composable
fun MeEmailVerificationSentBanner(
    emailAddress: String,
) {
    MeEmailVerificationContainer {
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
            text = stringResource(
                R.string.me_email_verification_sent_description,
                emailAddress
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Banner when requesting a verification link results in an error
 */
@Composable
fun MeEmailVerificationErrorBanner(
    onResendLinkClick: () -> Unit,
) {
    MeEmailVerificationContainer {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cross_in_circle_white_24dp),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.error),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        }

        Text(
            text = stringResource(R.string.me_email_verification_error_description),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )

        Text(
            text = stringResource(R.string.retry),
            style = MaterialTheme.typography.bodyLarge,
            color = colorResource(R.color.jetpack_green_50),
            modifier = Modifier
                .padding(top = 8.dp)
                .clickable {
                    onResendLinkClick()
                }
        )
    }
}

@Composable
private fun MeEmailVerificationContainer(
    content: @Composable () -> Unit,
) {
    AppThemeM3 {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeEmailUnverifiedPreview() {
    MeEmailUnverifiedBanner(
        emailAddress = "vonnegut@example.com",
        onSendLinkClick = {}
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeEmailVerificationRequestedPreview() {
    MeEmailVerificationSendingBanner(
        emailAddress = "vonnegut@example.com",
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeEmailVerificationSentPreview() {
    MeEmailVerificationSentBanner(
        emailAddress = "vonnegut@example.com",
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MeEmailVerificationErrorPreview() {
    MeEmailVerificationErrorBanner(
        onResendLinkClick = {}
    )
}
