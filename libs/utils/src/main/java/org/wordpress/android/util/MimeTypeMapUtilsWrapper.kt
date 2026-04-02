package org.wordpress.android.util

import android.webkit.MimeTypeMap
import dagger.Reusable
import javax.inject.Inject

@Reusable
class MimeTypeMapUtilsWrapper @Inject constructor() {
    fun getFileExtensionFromUrl(url: String?) = MimeTypeMap.getFileExtensionFromUrl(url)
    fun getSingleton() = MimeTypeMap.getSingleton()
}
