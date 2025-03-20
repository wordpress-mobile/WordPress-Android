package org.wordpress.android.ui.main.emailverificationbanner

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.ComposeView
import org.wordpress.android.ui.main.emailverificationbanner.EmailVerificationViewModel.VerificationState

/**
 * Custom view for Me screen email verification banner
 */
class EmailVerificationBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    private val composeView: ComposeView = ComposeView(context)

    private var verificationState: VerificationState? = null
    private var emailAddress: String = ""
    private var errorMessage: String = ""
    private var onSendLinkClick: () -> Unit = {}

    private val animDuration =
        context.resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

    init {
        addView(composeView)
    }

    fun setViewModel(viewModel: EmailVerificationViewModel) {
        visibility = View.VISIBLE
        composeView.setContent {
            EmailVerificationBanner(
                verificationState = viewModel.verificationState.collectAsState(),
                emailAddress = viewModel.emailAddress.collectAsState(),
                errorMessage = viewModel.errorMessage.collectAsState(),
                onSendLinkClick = {
                    viewModel.onSendVerificationLinkClick()
                }
            )
        }
    }

    /*fun setVerificationState(
        verificationState: VerificationState,
        emailAddress: String = "",
        errorMessage: String = "",
        onSendLinkClick: () -> Unit = {},
    ) {
        this.verificationState = verificationState
        this.emailAddress = emailAddress
        this.errorMessage = errorMessage
        this.onSendLinkClick = onSendLinkClick

        when (verificationState) {
            VerificationState.UNVERIFIED,
            VerificationState.LINK_REQUESTED,
            VerificationState.LINK_SENT,
            VerificationState.LINK_ERROR -> {
                if (isVisible) {
                    fadeOut(true)
                } else {
                    fadeInAndUpdate()
                }
            }

            VerificationState.VERIFIED,
            VerificationState.NO_ACCOUNT -> {
                if (visibility != View.GONE) {
                    fadeOut(false)
                }
            }
        }
    }

    private fun updateContent() {
        composeView.setContent {
            EmailVerificationBanner(
                verificationState = verificationState!!,
                emailAddress = emailAddress,
                errorMessage = errorMessage,
                onSendLinkClick = {
                    this.onSendLinkClick()
                }
            )
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
    }*/
}
