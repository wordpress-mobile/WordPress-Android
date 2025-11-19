package org.wordpress.android.support.he.model

import java.io.File

sealed class VideoDownloadState {
    object Idle : VideoDownloadState()
    object Downloading : VideoDownloadState()
    data class Success(val file: File) : VideoDownloadState()
    data class Error(val message: String) : VideoDownloadState()
}
