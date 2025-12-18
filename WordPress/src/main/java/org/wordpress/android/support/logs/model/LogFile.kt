package org.wordpress.android.support.logs.model

import java.io.File

data class LogFile(
    val file: File,
    val fileName: String,
    val title: String,
    val subtitle: String,
    // Log lines could be truncated to avoid memory issues
    val logLines: List<String>? = null
)
