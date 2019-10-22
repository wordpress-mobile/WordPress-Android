package org.wordpress.android.util

import android.content.Context
import android.net.Uri
import dagger.Reusable
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject

/**
 * Injectable wrapper around FluxCUtilsWrapper.
 *
 * FluxCUtilsWrapper interface is consisted of static methods, which make the client code difficult to test/mock.
 * Main purpose of this wrapper is to make testing easier.
 *
 */
@Reusable
class FluxCUtilsWrapper @Inject constructor(private val appContext: Context) {
    fun mediaModelFromLocalUri(
        uri: Uri,
        mimeType: String?,
        mediaStore: MediaStore,
        id: Int
    ): MediaModel? = FluxCUtils.mediaModelFromLocalUri(appContext, uri, mimeType, mediaStore, id)
}
