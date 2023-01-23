package org.wordpress.android.ui.posts.editor

import android.content.Context
import dagger.Reusable
import org.wordpress.android.editor.AztecEditorFragment
import javax.inject.Inject

/**
 * Injectable wrapper around AztecEditorFragment.
 *
 * AppPrefs interface contains some static methods, which make the client code difficult to test/mock. Main purpose of
 * this wrapper is to make testing of these static methods easier.
 *
 */
@Reusable
class AztecEditorFragmentStaticWrapper @Inject constructor(private val appContext: Context) {
    fun isMediaInPostBody(postContent: String, localMediaId: String) =
        AztecEditorFragment.isMediaInPostBody(appContext, postContent, localMediaId)
}
