package org.wordpress.android.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.io.FileReader
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.util.helpers.logfile.LogFileCleaner
import org.wordpress.android.util.helpers.logfile.LogFileProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileCleanerTest {
    lateinit var logFileProvider: LogFileProvider
    private val maxFiles = 10

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        logFileProvider = LogFileProvider.fromContext(context)

        repeat(maxFiles) {
            val file = File(logFileProvider.getLogFileDirectory(), "$it.log")
            file.writeText("$it")
            file.setLastModified((it * 10_000L).toLong())
        }

        assert(logFileProvider.getLogFileDirectory().listFiles().count() == maxFiles)
    }

    @After
    fun tearDown() {
        // Delete the test directory after each test
        logFileProvider.getLogFileDirectory().deleteRecursively()
    }

    @Test
    fun testThatCleanerPreservesMostRecentlyCreatedFiles() {
        LogFileCleaner(logFileProvider, 3).clean()

        val remainingFileIds = logFileProvider.getLogFileDirectory().listFiles().map {
            FileReader(it).readText()
        }.joinToString(",") // Strings are easier to assert against than arrays

        // This assertion is based on the fact that the initial list (pre-clean) would've
        // been "0,1,2,3,4,5,6,7,8,9". Retaining the last 3 entries gives the following:
        assertEquals("7,8,9", remainingFileIds)
    }

    @Test
    fun testThatCleanerPreservesCorrectNumberOfFiles() {
        val numberOfFiles = Random.nextInt(maxFiles)
        LogFileCleaner(logFileProvider, numberOfFiles).clean()
        assertEquals(numberOfFiles, logFileProvider.getLogFileDirectory().listFiles().count())
    }

    @Test
    fun testThatCleanerErasesAllFilesIfGivenZero() {
        LogFileCleaner(logFileProvider, 0).clean()
        assert(logFileProvider.getLogFileDirectory().listFiles().isEmpty())
    }
}
