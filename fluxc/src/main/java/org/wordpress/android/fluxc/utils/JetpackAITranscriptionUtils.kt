package org.wordpress.android.fluxc.utils

import java.io.File
import javax.inject.Inject

class JetpackAITranscriptionUtils @Inject constructor() {
    fun isFileEligibleForTranscription(file: File, sizeLimit: Long): Boolean {
        if (!fileExistsAndIsReadable(file)) {
            return false
        }
        return fileMeetsSizeLimit(file.length(), sizeLimit)
    }

    private fun fileExistsAndIsReadable(file: File) = file.exists() && file.canRead()

    private fun fileMeetsSizeLimit(fileSizeInBytes: Long, sizeLimit: Long): Boolean {
        return fileSizeInBytes <= sizeLimit
    }
}
