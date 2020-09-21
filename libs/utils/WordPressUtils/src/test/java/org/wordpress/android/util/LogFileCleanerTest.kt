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

/**
 *  The number of test files to create for each test run
 */
private const val MAX_FILES = 10

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileCleanerTest {
    private lateinit var logFileProvider: LogFileProvider

    @Before
    fun setup() {
        val context: Context = ApplicationProvider.getApplicationContext()
        logFileProvider = LogFileProvider.fromContext(context)

        repeat(MAX_FILES) {
            val file = File(logFileProvider.getLogFileDirectory(), "$it.log")
            file.writeText("$it")
            file.setLastModified(it * 10_000L)
        }

        assert(logFileProvider.getLogFileDirectory().listFiles().count() == MAX_FILES)
    }

    @After
    fun tearDown() {
        // Delete the test directory after each test
        logFileProvider.getLogFileDirectory().deleteRecursively()
    }

    @Test
    fun testThatCleanerPreservesMostRecentlyCreatedFiles() {
        val maxLogFileCount = Random.nextInt(MAX_FILES)
        LogFileCleaner(logFileProvider, maxLogFileCount).clean()

        // Strings are easier to assert against than arrays
        val remainingFileIds = logFileProvider.getLogFiles().joinToString(",") {
            FileReader(it).readText()
        }

        val expectedValue = (MAX_FILES - 1 downTo 0).take(maxLogFileCount).reversed().joinToString(",")
        assertEquals(expectedValue, remainingFileIds)
    }

    @Test
    fun testThatCleanerPreservesCorrectNumberOfFiles() {
        val numberOfFiles = Random.nextInt(MAX_FILES)
        LogFileCleaner(logFileProvider, numberOfFiles).clean()
        assertEquals(numberOfFiles, logFileProvider.getLogFileDirectory().listFiles().count())
    }

    @Test
    fun testThatCleanerErasesAllFilesIfGivenZero() {
        LogFileCleaner(logFileProvider, 0).clean()
        assert(logFileProvider.getLogFileDirectory().listFiles().isEmpty())
    }
}
