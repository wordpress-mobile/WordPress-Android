
package org.wordpress.android.imageeditor

import android.content.Context
import android.content.Intent
import org.wordpress.android.imageeditor.fragments.MainImageFragment

class ImageEditor {
    var imageUrls: ArrayList<String> = ArrayList()

    val isSingleImageAndCapability: Boolean
        get() = imageUrls.size == 1

    /**
     * @param context self explanatory
     * @param contentUri URI of initial media - can be local or remote
     */
    fun edit(
        context: Context,
        contentUri: String
    ) {
        // TODO
        // Temporarily goes to edit image activity
        val intent = Intent(context, EditImageActivity::class.java)
        intent.putExtra(MainImageFragment.ARG_MEDIA_CONTENT_URI, contentUri)

        EditImageActivity.startIntent(context, intent)
    }
}
