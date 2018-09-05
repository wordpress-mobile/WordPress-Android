package org.wordpress.android.util

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableRange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormattableContentUtils @Inject constructor() {
    fun getHeaderTextOrEmpty(header: FormattableContent?): String? {
        return header?.text ?: ""
    }

    fun getRangeSiteIdOrZero(header: FormattableContent?, rangeIndex: Int): Long {
        val range = getRangeOrNull(header, rangeIndex)
        return range?.siteId ?: 0L
    }

    fun getRangeIdOrZero(header: FormattableContent?, rangeIndex: Int): Long {
        val range = getRangeOrNull(header, rangeIndex)
        return range?.id ?: 0L
    }

    fun getRangeUrlOrEmpty(header: FormattableContent?, rangeIndex: Int): String? {
        val range = getRangeOrNull(header, rangeIndex)
        return range?.url ?: ""
    }

    fun getRangeOrNull(header: FormattableContent?, rangeIndex: Int): FormattableRange? {
        return header?.ranges?.let {
            return if (rangeIndex < it.size) it[rangeIndex] else null
        }
    }

    fun getMediaUrlOrEmpty(header: FormattableContent?, mediaIndex: Int): String? {
        val media = getMediaOrNull(header, mediaIndex)
        return media?.url ?: ""
    }

    fun getMediaOrNull(header: FormattableContent?, mediaIndex: Int): FormattableMedia? {
        return header?.media?.let {
            return if (mediaIndex < it.size) it[mediaIndex] else null
        }
    }
}
