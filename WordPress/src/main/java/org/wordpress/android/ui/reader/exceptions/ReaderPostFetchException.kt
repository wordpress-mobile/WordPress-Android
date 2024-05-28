package org.wordpress.android.ui.reader.exceptions

class ReaderPostFetchException(
    message: String = "Failed to fetch post(s).",
) : RuntimeException(message)
