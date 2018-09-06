package org.wordpress.android.util

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableRange
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FormattableContentUtils @Inject constructor() {
    fun getTextOrEmpty(content: FormattableContent?): String {
        return content?.text ?: ""
    }

    fun getRangeSiteIdOrZero(content: FormattableContent?, rangeIndex: Int): Long {
        val range = getRangeOrNull(content, rangeIndex)
        return range?.siteId ?: 0L
    }

    fun getRangeIdOrZero(content: FormattableContent?, rangeIndex: Int): Long {
        val range = getRangeOrNull(content, rangeIndex)
        return range?.id ?: 0L
    }

    fun getRangeUrlOrEmpty(content: FormattableContent?, rangeIndex: Int): String {
        val range = getRangeOrNull(content, rangeIndex)
        return range?.url ?: ""
    }

    fun getRangeOrNull(content: FormattableContent?, rangeIndex: Int): FormattableRange? {
        return content?.ranges?.let {
            return if (rangeIndex < it.size) it[rangeIndex] else null
        }
    }

    fun getMediaUrlOrEmpty(content: FormattableContent?, mediaIndex: Int): String {
        val media = getMediaOrNull(content, mediaIndex)
        return media?.url ?: ""
    }

    fun getMediaOrNull(content: FormattableContent?, mediaIndex: Int): FormattableMedia? {
        return content?.media?.let {
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
        return getRangeOrNull(content, rangeIndex)?.value ?: ""
    }

    fun getMetaIdsSiteIdOrZero(content: FormattableContent?): Long {
        return content?.meta?.ids?.site ?: 0
    }

    fun getMetaIdsUserIdOrZero(content: FormattableContent?): Long {
        return content?.meta?.ids?.user ?: 0
    }
}
