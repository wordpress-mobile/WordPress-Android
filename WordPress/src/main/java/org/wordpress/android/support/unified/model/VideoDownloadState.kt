package org.wordpress.android.support.unified.model

import java.io.File

sealed class VideoDownloadState {
    object Idle : VideoDownloadState()
    object Downloading : VideoDownloadState()
    object Error : VideoDownloadState()
    data class Success(val file: File) : VideoDownloadState()
}
