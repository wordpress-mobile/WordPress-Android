package org.wordpress.android.ui.reader.utils

import dagger.Reusable
import javax.inject.Inject

@Reusable
class ReaderImageScannerProvider @Inject constructor() {
    fun createReaderImageScanner(postContent: String, isPrivate: Boolean) = ReaderImageScanner(postContent, isPrivate)
}
