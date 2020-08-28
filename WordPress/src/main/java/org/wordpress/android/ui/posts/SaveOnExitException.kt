package org.wordpress.android.ui.posts

/**
 * This exception gets sent off to Sentry, and we are using distinct nested classes to make sure
 * Sentry does not group these exceptions together.
 */
sealed class SaveOnExitException(message: String) : Exception(message) {
    /**
     * It is a known issue that this exception may occur if a user exits the editor _very_ quickly after updating
     * the content of a post because the debouncer from the changes does not get a chance to fire. Therefore, we
     * expect this exception to occur occasionally.
     */
    class AutosavePending : SaveOnExitException(
            "Post had changes that needed to be saved when exiting the editor, but there was an autosave pending. " +
                    "This can occur when the user makes changes just before exiting the editor and the autosave " +
                    "debouncer does not have time to fire."
    )

    /**
     * If this exception occurs then it indicates that the autosave mechanism failed because there were
     * changes to the post content and the autosave mechanism was not preparing to save the post.
     */
    class NoAutosavePending : SaveOnExitException(
            "Post had changes that needed to be saved when exiting the editor, and there was NOT an autosave " +
                    "pending. This means that the editor's autosave mechanism failed."
    )

    companion object {
        fun build(isAutosavePending: Boolean): SaveOnExitException = if (isAutosavePending) {
            AutosavePending()
        } else {
            NoAutosavePending()
        }
    }
}
