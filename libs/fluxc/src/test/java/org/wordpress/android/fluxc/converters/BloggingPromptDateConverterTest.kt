package org.wordpress.android.fluxc.converters

import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.persistence.coverters.BloggingPromptDateConverter

class BloggingPromptDateConverterTest {
    @Test
    fun testBloggingPromptDateStringToDateObject() {
        val converter = BloggingPromptDateConverter()

        val date = "2022-05-01"

        // A string date converted to Date and back should be unaltered
        val result = converter.stringToDate(date)
        Assert.assertEquals(date, converter.dateToString(result))
    }
}
