package org.wordpress.android.ui.posts.navigation

/**
 * Represents the available destinations in the Edit Post flow.
 * Provides type-safe navigation between different screens in the post editing interface.
 */
sealed class EditPostDestination {
    /**
     * The main editor screen where users write and edit post content.
     */
    object Editor : EditPostDestination()

    /**
     * The settings screen for configuring post metadata, categories, tags, etc.
     */
    object Settings : EditPostDestination()

    /**
     * The publish settings screen for scheduling and publish options.
     */
    object PublishSettings : EditPostDestination()

    /**
     * The post history/revisions screen showing previous versions.
     */
    object History : EditPostDestination()

    companion object {
        /**
         * Returns the default destination for the Edit Post flow.
         * Used as a fallback when navigation state is unclear.
         */
        fun default(): EditPostDestination = Editor
    }
}
