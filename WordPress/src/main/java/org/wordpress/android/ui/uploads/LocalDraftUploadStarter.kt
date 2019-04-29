package org.wordpress.android.ui.uploads

import android.content.Context
import javax.inject.Singleton

@Singleton
class LocalDraftUploadStarter(
    private val context: Context
) {
    fun uploadLocalDrafts() {
        print("Uploading")
    }
}
