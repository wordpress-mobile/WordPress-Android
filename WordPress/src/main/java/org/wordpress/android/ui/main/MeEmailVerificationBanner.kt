package org.wordpress.android.ui.main

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import org.wordpress.android.R

/**
 * Custom view for Me screen email verification banner
 */
class MeEmailVerificationBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val composeView: ComposeView = ComposeView(context)

    private var verificationState: MeViewModel.EmailVerificationState? = null
    private var emailAddress: String = ""
    private var onLinkClick: (context: Context) -> Unit = {}

    private val animDuration =
        context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    init {
        addView(composeView)
    }

    fun setVerificationState(
        verificationState: MeViewModel.EmailVerificationState,
        emailAddress: String = "",
        onLinkClick: (context: Context) -> Unit = {},
    ) {
        this.verificationState = verificationState
        this.emailAddress = emailAddress
        this.onLinkClick = onLinkClick

        when (verificationState) {
            MeViewModel.EmailVerificationState.UNVERIFIED,
            MeViewModel.EmailVerificationState.LINK_REQUESTED,
            MeViewModel.EmailVerificationState.LINK_SENT,
            MeViewModel.EmailVerificationState.ERROR -> {
                if (isVisible) {
                    fadeOut(true)
                } else {
                    fadeInAndUpdate()
                }
            }

            MeViewModel.EmailVerificationState.VERIFIED,
            MeViewModel.EmailVerificationState.NO_ACCOUNT -> {
                if (isVisible.not()) {
                    fadeOut(false)
                }
            }
        }
    }

    private fun updateContent() {
        when (verificationState!!) {
            MeViewModel.EmailVerificationState.UNVERIFIED -> {
                composeView.setContent {
                    MeEmailUnverifiedBanner(
                        emailAddress = emailAddress,
                        onSendLinkClick = {
                            onLinkClick(context)
                        }
                    )
                }
            }

            MeViewModel.EmailVerificationState.LINK_REQUESTED -> {
                composeView.setContent {
                    MeEmailVerificationSendingBanner(
                        emailAddress = emailAddress
                    )
                }
            }

            MeViewModel.EmailVerificationState.LINK_SENT -> {
                composeView.setContent {
                    MeEmailVerificationSentBanner(
                        emailAddress = emailAddress
                    )
                }
            }

            MeViewModel.EmailVerificationState.ERROR -> {
                composeView.setContent {
                    MeEmailVerificationErrorBanner(
                        onResendLinkClick = {
                            onLinkClick(context)
                        }
                    )
                }
            }

            else -> {
                // do nothing
            }
        }
    }

    private fun fadeInAndUpdate() {
        with(ObjectAnimator.ofFloat(this, ALPHA, 0.0f, 1.0f)) {
            setDuration(animDuration)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    this@MeEmailVerificationBanner.isVisible = true
                    updateContent()
                }
            })
            start()
        }
    }

    private fun fadeOut(updateAfterFadeOut: Boolean) {
        with(ObjectAnimator.ofFloat(this, ALPHA, 1.0f, 0.0f)) {
            setDuration(animDuration)
            interpolator = LinearInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (updateAfterFadeOut) {
                        fadeInAndUpdate()
                    } else {
                        this@MeEmailVerificationBanner.isVisible = false
                    }
                }
            })
            start()
        }
    }
}

/**
 * Banner for Me screen when user's email hasn't yet been verified
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
 * Banner for Me screen when user's email hasn't yet been verified but a verification link has been requested
 */
@Composable
fun MeEmailVerificationSendingBanner(
    emailAddress: String,
) {
    MeEmailVerificationContainer {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_gridicons_checkmark_circle),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = stringResource(R.string.me_email_verification_sending),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

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

/**
 * Banner for Me screen when user's email hasn't yet been verified but a verification link has been sent
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
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

/**
 * Banner for Me screen when requesting a verification link results in an error
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        content()
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
    MeEmailVerificationSendingBanner(
        emailAddress = "vonnegut@example.com",
    )
}

@Preview
@Composable
fun MeEmailVerificationSentPreview() {
    MeEmailVerificationSentBanner(
        emailAddress = "vonnegut@example.com",
    )
}

@Preview
@Composable
fun MeEmailVerificationErrorPreview() {
    MeEmailVerificationErrorBanner(
        onResendLinkClick = {}
    )
}
