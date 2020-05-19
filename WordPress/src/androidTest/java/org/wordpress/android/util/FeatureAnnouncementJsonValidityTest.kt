package org.wordpress.android.util

import android.content.Context
import android.text.TextUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import org.wordpress.android.ui.whatsnew.FeatureAnnouncements
import java.io.FileNotFoundException

class FeatureAnnouncementJsonValidityTest {
    @Test
    fun testValidityOfFeatureAnnouncementJsonFile() {
        var featureAnnouncementFileContent: String? = null

        val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            featureAnnouncementFileContent = context.assets.open("FEATURE_ANNOUNCEMENTS.json")
                    .bufferedReader().use { it.readText() }
        } catch (fileNotFound: FileNotFoundException) {
            fail("FEATURE_ANNOUNCEMENTS.json is missing")
        }

        assertEquals(TextUtils.isEmpty(featureAnnouncementFileContent), false)

        try {
            Gson().fromJson(
                    featureAnnouncementFileContent,
                    FeatureAnnouncements::class.java
            )
        } catch (jsonSyntaxException: JsonSyntaxException) {
            fail(
                    "JsonSyntaxException when parsing FEATURE_ANNOUNCEMENTS.json:" +
                            " ${jsonSyntaxException.message}"
            )
        }
    }
}
