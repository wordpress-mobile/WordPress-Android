package org.wordpress.android.ui.reader.exception

class ReaderPostFetchException(
    message: String = "Failed to fetch post(s).",
) : RuntimeException(message)
