package org.wordpress.android.ui.utils

import android.content.Intent
import org.wordpress.android.util.UriWrapper
import org.wordpress.android.viewmodel.ContextProvider
import javax.inject.Inject

class IntentUtils @Inject constructor(
    private val contextProvider: ContextProvider
) {
    /**
     * This function checks if we can find any Activity that can handle an [Intent] with the given action and URI.
     * If so, it returns true. Otherwise, it returns false.
     */
    fun canResolveWith(action: String, uri: UriWrapper): Boolean =
        Intent(action, uri.uri).resolveActivity(contextProvider.getContext().packageManager) != null
}
