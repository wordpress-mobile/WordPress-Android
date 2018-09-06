package org.wordpress.android.util

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableRange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormattableContentUtils @Inject constructor() {
    fun getTextOrEmpty(header: FormattableContent?): String {
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

    fun getRangeUrlOrEmpty(header: FormattableContent?, rangeIndex: Int): String {
        val range = getRangeOrNull(header, rangeIndex)
        return range?.url ?: ""
    }

    fun getRangeOrNull(header: FormattableContent?, rangeIndex: Int): FormattableRange? {
        return header?.ranges?.let {
            return if (rangeIndex < it.size) it[rangeIndex] else null
        }
    }

    fun getMediaUrlOrEmpty(header: FormattableContent?, mediaIndex: Int): String {
        val media = getMediaOrNull(header, mediaIndex)
        return media?.url ?: ""
    }

    fun getMediaOrNull(header: FormattableContent?, mediaIndex: Int): FormattableMedia? {
        return header?.media?.let {
            return if (mediaIndex < it.size) it[mediaIndex] else null
        }
    }

    fun getMetaTitlesHomeOrEmpty(content: FormattableContent?): String {
        return content?.meta?.titles?.home ?: ""
    }

    fun getMetaLinksHomeOrEmpty(content: FormattableContent?): String {
        return content?.meta?.links?.home ?: ""
    }

    fun getMetaTitlesTaglineOrEmpty(content: FormattableContent?): String {
        return content?.meta?.titles?.tagline ?: ""
    }

    fun getRangeValueOrEmpty(content: FormattableContent?, rangeIndex: Int): String {
        return if (content?.ranges != null && content.ranges!!.size < rangeIndex) {
            content.ranges?.get(rangeIndex)?.value ?: ""
        } else ""
    }

    fun getMetaIdsSiteIdOrZero(content: FormattableContent?): Long {
        return content?.meta?.ids?.site ?: 0
    }

    fun getMetaIdsUserIdOrZero(content: FormattableContent?): Long {
        return content?.meta?.ids?.user ?: 0
    }
}
