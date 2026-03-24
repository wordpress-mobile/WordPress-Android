package org.wordpress.android.ui.reader.utils

import org.wordpress.android.util.StringUtils
import java.util.regex.Pattern
import androidx.core.net.toUri

object ReaderHtmlUtils {
    // regex for matching oriwidth attributes in tags
    private val ORIGINAL_WIDTH_ATTR_PATTERN: Pattern = Pattern.compile(
        "data-orig-size\\s*=\\s*['\"](.*?),.*?['\"]",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    private val ORIGINAL_HEIGHT_ATTR_PATTERN: Pattern = Pattern.compile(
        "data-orig-size\\s*=\\s*['\"].*?,(.*?)['\"]",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    // regex for matching width attributes in tags
    private val WIDTH_ATTR_PATTERN: Pattern = Pattern.compile(
        "width\\s*=\\s*['\"](.*?)['\"]",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    // regex for matching height attributes in tags
    private val HEIGHT_ATTR_PATTERN: Pattern = Pattern.compile(
        "height\\s*=\\s*['\"](.*?)['\"]",
        Pattern.DOTALL or Pattern.CASE_INSENSITIVE
    )

    /*
     * returns the integer value from the data-orig-size attribute in the passed html tag
     */
    fun getOriginalWidthAttrValue(tag: String): Int {
        return StringUtils.stringToInt(
            matchTagAttrPattern(
                ORIGINAL_WIDTH_ATTR_PATTERN, tag
            ), 0
        )
    }

    fun getOriginalHeightAttrValue(tag: String): Int {
        return StringUtils.stringToInt(
            matchTagAttrPattern(
                ORIGINAL_HEIGHT_ATTR_PATTERN, tag
            ), 0
        )
    }

    /*
     * returns the integer value from the width attribute in the passed html tag
     */
    fun getWidthAttrValue(tag: String): Int {
        return StringUtils.stringToInt(
            matchTagAttrPattern(
                WIDTH_ATTR_PATTERN, tag
            ), 0
        )
    }

    fun getHeightAttrValue(tag: String): Int {
        return StringUtils.stringToInt(
            matchTagAttrPattern(
                HEIGHT_ATTR_PATTERN, tag
            ), 0
        )
    }

    /*
     * returns the integer value of the passed query param in the passed url - returns zero
     * if the url is invalid, or the param doesn't exist, or the param value could not be
     * converted to an int
     */
    fun getIntQueryParam(
        url: String,
        param: String
    ): Int {
        if (!url.startsWith("http")
            || !url.contains("$param=")
        ) {
            return 0
        }
        return StringUtils.stringToInt(url.toUri().getQueryParameter(param))
    }

    private fun matchTagAttrPattern(pattern: Pattern, tag: String): String? {
        val matcher = pattern.matcher(tag)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }

    fun interface HtmlScannerListener {
        fun onTagFound(tag: String, src: String)
    }
}
