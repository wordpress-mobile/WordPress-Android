package org.wordpress.android.ui.main.emailverificationbanner

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState

@Composable
fun EmailVerificationBanner(
    verificationState: VerificationState,
    emailAddress: String = "",
    errorMessage: String = "",
    onSendLinkClick: () -> Unit = {},
) {
    when (verificationState) {
        VerificationState.UNVERIFIED -> {
            EmailUnverifiedBanner(
                onSendLinkClick = {
                    onSendLinkClick()
                }
            )
        }

        VerificationState.LINK_REQUESTED -> {
            EmailVerificationSendingBanner()
        }

        VerificationState.LINK_SENT -> {
            EmailVerificationSentBanner(
                emailAddress = emailAddress,
                onResendLinkClick = {
                    onSendLinkClick()
                }
            )
        }

        VerificationState.LINK_ERROR -> {
            EmailVerificationErrorBanner(
                errorMessage = errorMessage,
                onRetrySendLinkClick = {
                    onSendLinkClick()
                }
            )
        }

        else -> {
            // do nothing
        }
    }
}

/**
 * Banner when user's email hasn't yet been verified
 */
@Composable
private fun EmailUnverifiedBanner(
    onSendLinkClick: () -> Unit,
) {
    EmailVerificationContainer {
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
            text = stringResource(R.string.me_email_verification_verify_email_description),
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
private fun EmailVerificationSendingBanner() {
    EmailVerificationContainer {
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
                text = stringResource(R.string.me_email_verification_sending_description),
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
private fun EmailVerificationSentBanner(
    emailAddress: String,
    onResendLinkClick: () -> Unit,
) {
    EmailVerificationContainer {
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

        Text(
            text = stringResource(R.string.me_email_verification_resend),
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

/**
 * Banner when requesting a verification link results in an error
 */
@Composable
private fun EmailVerificationErrorBanner(
    errorMessage: String = "",
    onRetrySendLinkClick: () -> Unit,
) {
    EmailVerificationContainer {
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
            text = errorMessage.takeIf {
                errorMessage.isNotEmpty()
            } ?: stringResource(R.string.me_email_verification_generic_error),
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
                    onRetrySendLinkClick()
                }
        )
    }
}

@Composable
private fun EmailVerificationContainer(
    content: @Composable () -> Unit,
) {
    AppThemeM3 {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.gravatar_info_banner),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(20.dp)
        ) {
            content()
        }
    }
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailUnverifiedPreview() {
    EmailUnverifiedBanner(
        onSendLinkClick = {}
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationRequestedPreview() {
    EmailVerificationSendingBanner()
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationSentPreview() {
    EmailVerificationSentBanner(
        emailAddress = "vonnegut@example.com",
        onResendLinkClick = {}
    )
}

@Preview
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationErrorPreview() {
    EmailVerificationErrorBanner(
        onRetrySendLinkClick = {}
    )
}
