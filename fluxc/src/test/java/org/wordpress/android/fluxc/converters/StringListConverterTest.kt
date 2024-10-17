package org.wordpress.android.fluxc.converters

import org.junit.Assert
import org.junit.Test
import org.wordpress.android.fluxc.persistence.coverters.StringListConverter

class StringListConverterTest {
    @Test
    fun testStringToListToStringConversion() {
        val converter = StringListConverter()

        val string = "apple,banana,1"

        // A string converted to list and back should be unaltered
        val result = converter.stringToList(string)
        Assert.assertEquals(string, converter.listToString(result))
    }
}
