package org.wordpress.android.ui.reader

object ReaderInterfaces {
    /*
     * called from post detail fragment so toolbar can animate in/out when scrolling
     */
    interface AutoHideToolbarListener {
        fun onShowHideToolbar(show: Boolean)
    }

    /*
     * implemented by the activity hosting a blog preview, which can't be shown if its blog info can't be loaded
     */
    interface OnBlogInfoFailedListener {
        fun onBlogInfoFailed()
    }
}
