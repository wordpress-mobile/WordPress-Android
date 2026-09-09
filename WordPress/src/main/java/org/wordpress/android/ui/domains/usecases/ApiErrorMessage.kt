package org.wordpress.android.ui.domains.usecases

import rs.wordpress.api.kotlin.WpRequestResult

/**
 * The message WordPress.com sent with a refusal, for the screens that put it in
 * front of the user.
 *
 * Null for a failure that never reached the API, such as an offline device or a
 * response that did not parse. `WpRequestErrorLogger` records those.
 */
internal fun WpRequestResult<*>.apiErrorMessage(): String? =
    (this as? WpRequestResult.WpError)?.errorMessage
