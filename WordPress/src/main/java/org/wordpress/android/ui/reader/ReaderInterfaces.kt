package org.wordpress.android.ui.reader

object ReaderInterfaces {
    /*
     * implemented by the activity hosting a blog preview, which can't be shown if its blog info can't be loaded
     */
    interface OnBlogInfoFailedListener {
        fun onBlogInfoFailed()
    }
}
