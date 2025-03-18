package org.wordpress.android.ui.main.emailverificationbanner

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.isVisible

/**
 * Custom view for Me screen email verification banner
 */
class EmailVerificationBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val composeView: ComposeView = ComposeView(context)

    private var verificationState: EmailVerificationViewModel.EmailVerificationState? = null
    private var emailAddress: String = ""
    private var onLinkClick: (context: Context) -> Unit = {}

    private val animDuration =
        context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    init {
        addView(composeView)
    }

    fun setVerificationState(
        verificationState: EmailVerificationViewModel.EmailVerificationState,
        emailAddress: String = "",
        onLinkClick: (context: Context) -> Unit = {},
    ) {
        this.verificationState = verificationState
        this.emailAddress = emailAddress
        this.onLinkClick = onLinkClick

        when (verificationState) {
            EmailVerificationViewModel.EmailVerificationState.UNVERIFIED,
            EmailVerificationViewModel.EmailVerificationState.LINK_REQUESTED,
            EmailVerificationViewModel.EmailVerificationState.LINK_SENT,
            EmailVerificationViewModel.EmailVerificationState.LINK_ERROR -> {
                if (isVisible) {
                    fadeOut(true)
                } else {
                    fadeInAndUpdate()
                }
            }

            EmailVerificationViewModel.EmailVerificationState.VERIFIED,
            EmailVerificationViewModel.EmailVerificationState.NO_ACCOUNT -> {
                if (isVisible.not()) {
                    fadeOut(false)
                }
            }
        }
    }

    private fun updateContent() {
        when (verificationState!!) {
            EmailVerificationViewModel.EmailVerificationState.UNVERIFIED -> {
                composeView.setContent {
                    EmailUnverifiedBanner(
                        emailAddress = emailAddress,
                        onSendLinkClick = {
                            onLinkClick(context)
                        }
                    )
                }
            }

            EmailVerificationViewModel.EmailVerificationState.LINK_REQUESTED -> {
                composeView.setContent {
                    EmailVerificationSendingBanner(
                        emailAddress = emailAddress
                    )
                }
            }

            EmailVerificationViewModel.EmailVerificationState.LINK_SENT -> {
                composeView.setContent {
                    EmailVerificationSentBanner(
                        emailAddress = emailAddress
                    )
                }
            }

            EmailVerificationViewModel.EmailVerificationState.LINK_ERROR -> {
                composeView.setContent {
                    EmailVerificationErrorBanner(
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
                    this@EmailVerificationBanner.isVisible = true
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
                        this@EmailVerificationBanner.isVisible = false
                    }
                }
            })
            start()
        }
    }
}
