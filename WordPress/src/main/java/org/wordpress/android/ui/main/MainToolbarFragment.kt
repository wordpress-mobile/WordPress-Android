package org.wordpress.android.ui.main

/**
 * Interface for use with a fragment hosting a toolbar or actionbar.
 */
interface MainToolbarFragment {
    /**
     * Set the title in the active fragment
     * @param title The title to set on the toolbar
     */
    fun setTitle(title: String)
}
