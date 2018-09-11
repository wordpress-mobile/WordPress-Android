package org.wordpress.android.util

import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableMedia
import org.wordpress.android.fluxc.tools.FormattableRange

fun FormattableContent?.getTextOrEmpty(): String {
    return this?.text ?: ""
}

fun FormattableContent?.getRangeSiteIdOrZero(rangeIndex: Int): Long {
    val range = getRangeOrNull(rangeIndex)
    return range?.siteId ?: 0L
}

fun FormattableContent?.getRangeIdOrZero(rangeIndex: Int): Long {
    val range = getRangeOrNull(rangeIndex)
    return range?.id ?: 0L
}

fun FormattableContent?.getRangeUrlOrEmpty(rangeIndex: Int): String {
    val range = getRangeOrNull(rangeIndex)
    return range?.url ?: ""
}

fun FormattableContent?.getRangeOrNull(rangeIndex: Int): FormattableRange? {
    return this?.ranges?.let {
        return if (rangeIndex < it.size) it[rangeIndex] else null
    }
}

fun FormattableContent?.getMediaUrlOrEmpty(mediaIndex: Int): String {
    val media = getMediaOrNull(mediaIndex)
    return media?.url ?: ""
}

fun FormattableContent?.getMediaOrNull(mediaIndex: Int): FormattableMedia? {
    return this?.media?.let {
        return if (mediaIndex < it.size) it[mediaIndex] else null
    }
}

fun FormattableContent?.getMetaTitlesHomeOrEmpty(): String {
    return this?.meta?.titles?.home ?: ""
}

fun FormattableContent?.getMetaLinksHomeOrEmpty(): String {
    return this?.meta?.links?.home ?: ""
}

fun FormattableContent?.getMetaTitlesTaglineOrEmpty(): String {
    return this?.meta?.titles?.tagline ?: ""
}

fun FormattableContent?.getRangeValueOrEmpty(rangeIndex: Int): String {
    return getRangeOrNull(rangeIndex)?.value ?: ""
}

fun FormattableContent?.getMetaIdsSiteIdOrZero(): Long {
    return this?.meta?.ids?.site ?: 0
}

fun FormattableContent?.getMetaIdsUserIdOrZero(): Long {
    return this?.meta?.ids?.user ?: 0
}
