package org.wordpress.android.fluxc.utils

import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils

class BloggingPromptUtilsTest {
    @Test
    fun testBloggingPromptDateStringToDateObject() {
        val date = "2022-05-01"

        // A string date converted to Date and back should be unaltered
        val result = BloggingPromptsUtils.stringToDate(date)
        Assert.assertEquals(date, BloggingPromptsUtils.dateToString(result))
    }
}
