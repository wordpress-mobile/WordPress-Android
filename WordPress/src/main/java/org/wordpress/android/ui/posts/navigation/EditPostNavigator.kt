package org.wordpress.android.ui.posts.navigation

/**
 * Interface for handling navigation between different screens in the Edit Post flow.
 * Provides abstraction over the underlying navigation implementation.
 */
interface EditPostNavigator {
    /**
     * Navigate to the specified destination.
     */
    fun navigateTo(destination: EditPostDestination)

    /**
     * Get the currently active destination.
     */
    fun getCurrentDestination(): EditPostDestination

    /**
     * Check if back navigation is possible from current destination.
     */
    fun canNavigateBack(): Boolean

    /**
     * Handle back navigation. Returns true if navigation was handled, false otherwise.
     */
    fun navigateBack(): Boolean
}
