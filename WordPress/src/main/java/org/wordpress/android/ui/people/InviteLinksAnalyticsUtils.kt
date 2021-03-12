package org.wordpress.android.ui.people

import org.wordpress.android.ui.people.AnalyticsInviteLinksActionResult.ERROR
import org.wordpress.android.ui.people.AnalyticsInviteLinksActionResult.SUCCEEDED
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Failure
import org.wordpress.android.ui.people.InviteLinksApiCallsProvider.InviteLinksCallResult.Success

private const val INVITE_LINKS_ACTION_RESULT = "invite_links_action_result"
private const val INVITE_LINKS_ACTION_HAS_LINKS = "invite_links_action_has_links"
private const val INVITE_LINKS_ACTION_ERROR_MESSAGE = "invite_links_action_error_message"
private const val INVITE_LINKS_SHARED_ROLE = "invite_links_shared_role"

enum class AnalyticsInviteLinksActionResult(val actionResult: String) {
    SUCCEEDED("succeeded"),
    ERROR("error")
}

enum class AnalyticsInviteLinksGenericError(val errorMessage: String) {
    USER_NOT_AUTHENTICATED("user_not_authenticated"),
    NO_NETWORK("no_network"),
    NO_ROLE_DATA_MATCHED("no_role_data_matched")
}

fun MutableMap<String, Any?>.addInviteLinksActionResult(
    result: AnalyticsInviteLinksActionResult,
    errorMessage: String? = null
): MutableMap<String, Any?> {
    this[INVITE_LINKS_ACTION_RESULT] = result.actionResult
    errorMessage?.also {
        this[INVITE_LINKS_ACTION_ERROR_MESSAGE] = errorMessage
    }
    return this
}

fun MutableMap<String, Any?>.addInviteLinksActionResult(
    actionResult: InviteLinksCallResult,
    errorMessage: String? = null
): MutableMap<String, Any?> {
    this[INVITE_LINKS_ACTION_RESULT] = when (actionResult) {
        is Success -> SUCCEEDED.actionResult
        is Failure -> ERROR.actionResult
    }

    if (actionResult is Success) {
        this[INVITE_LINKS_ACTION_HAS_LINKS] = actionResult.links.count() > 0
    }

    errorMessage?.also {
        this[INVITE_LINKS_ACTION_ERROR_MESSAGE] = errorMessage
    }
    return this
}

fun MutableMap<String, Any?>.addInviteLinksSharedRole(
    role: String
): MutableMap<String, Any?> {
    return this.apply { this[INVITE_LINKS_SHARED_ROLE] = role }
}
