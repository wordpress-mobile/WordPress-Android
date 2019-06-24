package org.wordpress.android.fluxc.model.stats.time

data class FileDownloadsModel(val fileDownloads: List<FileDownloads>, val hasMore: Boolean) {
    data class FileDownloads(
        val name: String,
        val count: Int
    )
}
