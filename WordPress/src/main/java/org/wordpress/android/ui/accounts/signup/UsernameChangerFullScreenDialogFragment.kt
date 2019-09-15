package org.wordpress.android.ui.accounts.signup

import android.os.Bundle
import android.text.Html
import android.text.Spanned
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat.SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED
import org.wordpress.android.ui.FullScreenDialogFragment.FullScreenDialogController

/**
 * Implements functionality specific to the Username Changer functionality in the sign-up flow.
 */
class UsernameChangerFullScreenDialogFragment : BaseUsernameChangerFullScreenDialogFragment() {
    override fun getSuggestionsFailedStat() = SIGNUP_SOCIAL_EPILOGUE_USERNAME_SUGGESTIONS_FAILED
    override fun canHeaderTextLiveUpdate() = true
    override fun getHeaderText(username: String?, display: String?): Spanned = Html.fromHtml(
            String.format(
                    getString(R.string.username_changer_header),
                    "<b>",
                    username,
                    "</b>",
                    "<b>",
                    display,
                    "</b>"
            )
    )

    override fun onUsernameConfirmed(controller: FullScreenDialogController, usernameSelected: String) {
        val result = Bundle().apply { putString(RESULT_USERNAME, usernameSelected) }
        controller.confirm(result)
    }
}
