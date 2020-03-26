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
import org.wordpress.android.util.helpers.logfile.LogFileHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileCleanerTest {
    lateinit var testContext: Context
    lateinit var logFileDirectory: File

    private val maxFiles = 10

    @Before
    fun setup() {
        testContext = ApplicationProvider.getApplicationContext()
        logFileDirectory = LogFileHelpers.logFileDirectory(testContext)

        repeat(maxFiles) {
            val file = File(logFileDirectory, "$it.log")
            file.writeText("$it")
            file.setLastModified((it * 10_000L).toLong())
        }

        assert(logFileDirectory.listFiles().count() == maxFiles)
    }

    @After
    fun tearDown() {
        // Delete the test directory after each test
        logFileDirectory.deleteRecursively()
    }

    @Test
    fun testThatCleanerPreservesMostRecentlyCreatedFiles() {
        LogFileCleaner(testContext, 3).clean()

        val remainingFileIds = logFileDirectory.listFiles().map {
            FileReader(it).readText()
        }.joinToString(",") // Strings are easier to assert against than arrays

        // This assertion is based on the fact that the initial list (pre-clean) would've
        // been "0,1,2,3,4,5,6,7,8,9". Retaining the last 3 entries gives the following:
        assertEquals("7,8,9", remainingFileIds)
    }

    @Test
    fun testThatCleanerPreservesCorrectNumberOfFiles() {
        val numberOfFiles = Random.nextInt(maxFiles)
        LogFileCleaner(testContext, numberOfFiles).clean()
        assertEquals(numberOfFiles, logFileDirectory.listFiles().count())
    }

    @Test
    fun testThatCleanerErasesAllFilesIfGivenZero() {
        LogFileCleaner(testContext, 0).clean()
        assert(logFileDirectory.listFiles().isEmpty())
    }
}
