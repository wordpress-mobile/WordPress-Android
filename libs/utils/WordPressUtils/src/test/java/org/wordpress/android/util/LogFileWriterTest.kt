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
import org.wordpress.android.util.helpers.logfile.LogFileProvider
import org.wordpress.android.util.helpers.logfile.LogFileWriter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileWriterTest {
    private lateinit var testProvider: LogFileProvider

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        testProvider = LogFileProvider.fromContext(context)
    }

    @After
    fun tearDown() {
        // Delete the test directory after each test
        testProvider.getLogFileDirectory().deleteRecursively()
    }

    @Test
    fun testThatFileWriterCreatesLogFile() {
        val writer = LogFileWriter(testProvider)
        assert(writer.getFile().exists())
    }

    @Test
    fun testThatContentsAreWrittenToFile() {
        val randomString = UUID.randomUUID().toString()
        val writer = LogFileWriter(testProvider)
        writer.write(randomString)

        // Allow the async process to persist the file changes
        Thread.sleep(1000)

        val contents = FileReader(writer.getFile()).readText()
        assertEquals(randomString, contents)
    }
}
