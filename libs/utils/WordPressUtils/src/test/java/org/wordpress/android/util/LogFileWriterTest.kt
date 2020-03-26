package org.wordpress.android.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.io.FileReader
import java.util.UUID
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.util.helpers.logfile.LogFileHelpers
import org.wordpress.android.util.helpers.logfile.LogFileWriter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileWriterTest {
    lateinit var testContext: Context

    @Before
    fun setup() {
        testContext = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        // Delete the test directory after each test
        LogFileHelpers.logFileDirectory(testContext).deleteRecursively()
    }

    @Test
    fun testThatFileWriterCreatesLogFile() {
        val writer = LogFileWriter(testContext)
        assert(writer.file.exists())
    }

    @Test
    fun testThatContentsAreWrittenToFile() {
        val randomString = UUID.randomUUID().toString()
        val writer = LogFileWriter(testContext)
        writer.write(randomString)

        val contents = FileReader(writer.file).readText()
        assertEquals(randomString, contents)
    }
}
