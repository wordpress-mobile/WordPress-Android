package org.wordpress.android.ui.main.emailverificationbanner

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wordpress.android.R
import org.wordpress.android.ui.compose.theme.AppThemeM3
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState

private const val ANIM_ALPHA = 0.2f

@Composable
fun EmailVerificationBanner(
    verificationState: State<VerificationState?>,
    emailAddress: State<String>,
    errorMessage: State<String>,
    onSendLinkClick: () -> Unit = {},
) {
    // states that were initiated via user interaction should be announced to screen readers
    val announcementStringRes = when (verificationState.value) {
        VerificationState.LINK_REQUESTED -> {
            R.string.me_email_verification_sending
        }

        VerificationState.LINK_SENT -> {
            R.string.me_email_verification_sent
        }

        VerificationState.LINK_ERROR -> {
            R.string.me_email_verification_generic_error
        }

        else -> {
            null
        }
    }

    announcementStringRes?.let { stringRes ->
        val context = LocalContext.current
        val view = LocalView.current
        LaunchedEffect(verificationState.value) {
            view.announceForAccessibility(context.getString(stringRes))
        }
    }

    when (verificationState.value) {
        VerificationState.UNVERIFIED -> {
            EmailVerificationContainer(
                content = {
                    EmailUnverifiedBanner(
                        onSendLinkClick = {
                            onSendLinkClick()
                        }
                    )
                }
            )
        }

        VerificationState.LINK_REQUESTED -> {
            EmailVerificationContainer(
                content = {
                    EmailVerificationSendingBanner()
                }
            )
        }

        VerificationState.LINK_SENT -> {
            EmailVerificationContainer(
                content = {
                    EmailVerificationSentBanner(
                        emailAddress = emailAddress.value,
                        onResendLinkClick = {
                            onSendLinkClick()
                        }
                    )
                }
            )
        }

        VerificationState.LINK_ERROR -> {
            EmailVerificationContainer(
                content = {
                    EmailVerificationErrorBanner(
                        errorMessage = errorMessage.value,
                        onRetrySendLinkClick = {
                            onSendLinkClick()
                        }
                    )
                }
            )
        }

        else -> {
            // show nothing
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

/**
 * Banner when user's email hasn't been verified but a verification link has been requested
 */
@Composable
private fun EmailVerificationSendingBanner() {
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

/**
 * Banner when user's email hasn't yet been verified but a verification link has been sent
 */
@Composable
private fun EmailVerificationSentBanner(
    emailAddress: String,
    onResendLinkClick: () -> Unit,
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

/**
 * Banner when requesting a verification link results in an error
 */
@Composable
private fun EmailVerificationErrorBanner(
    errorMessage: String = "",
    onRetrySendLinkClick: () -> Unit,
) {
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

@Composable
private fun EmailVerificationContainer(
    content: @Composable () -> Unit,
) {
    // always make the banner initially visible in preview mode
    val isPreview = LocalInspectionMode.current
    val state = remember {
        MutableTransitionState(isPreview).apply {
            targetState = true
        }
    }

    AppThemeM3 {
        AnimatedVisibility(
            visibleState = state,
            enter = fadeIn(
                initialAlpha = ANIM_ALPHA
            ),
            exit = fadeOut(
                targetAlpha = ANIM_ALPHA
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = colorResource(R.color.gravatar_info_banner),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(20.dp)
                    .semantics(mergeDescendants = true) {}
            ) {
                content()
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailUnverifiedPreview() {
    EmailVerificationContainer(
        content = {
            EmailUnverifiedBanner(
                onSendLinkClick = {}
            )
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationRequestedPreview() {
    EmailVerificationContainer(
        content = {
            EmailVerificationSendingBanner()
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationSentPreview() {
    EmailVerificationContainer(
        content = {
            EmailVerificationSentBanner(
                emailAddress = "vonnegut@example.com",
                onResendLinkClick = {}
            )
        }
    )
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun EmailVerificationErrorPreview() {
    EmailVerificationContainer(
        content = {
            EmailVerificationErrorBanner(
                onRetrySendLinkClick = {}
            )
        }
    )
}
