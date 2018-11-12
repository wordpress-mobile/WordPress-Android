package org.wordpress.android.viewmodel.helpers

import android.content.Context
import android.support.annotation.StringRes
import android.support.v4.app.FragmentManager
import org.wordpress.android.ui.posts.BasicFragmentDialog

class DialogHolder(
    val tag: String,
    @StringRes val titleRes: Int,
    @StringRes val messageRes: Int,
    @StringRes val positiveButtonTextRes: Int,
    @StringRes val negativeButtonTextRes: Int
) {
    fun show(context: Context, fragmentManager: FragmentManager) {
        val dialog = BasicFragmentDialog()
        dialog.initialize(tag,
                context.getString(titleRes),
                context.getString(messageRes),
                context.getString(positiveButtonTextRes),
                context.getString(negativeButtonTextRes),
                null)
        dialog.show(fragmentManager, tag)
    }
}
