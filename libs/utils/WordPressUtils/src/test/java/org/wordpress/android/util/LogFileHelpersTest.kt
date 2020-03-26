package org.wordpress.android.util

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.UUID
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.util.helpers.logfile.LogFileHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileHelpersTest {
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
    fun testThatLogFileDirectoryIsCreatedIfNotExists() {
        val directory = LogFileHelpers.logFileDirectory(testContext)
        assert(directory.exists())
    }

    @Test
    fun testThatLogFilesListsAllFiles() {
        val directory = LogFileHelpers.logFileDirectory(testContext)
        File(directory, UUID.randomUUID().toString()).createNewFile()
        Assert.assertEquals(LogFileHelpers.logFiles(testContext).count(), 1)
    }

    @Test
    fun testThatLogFilesSortsFilesWithMostRecentFirst() {
        val directory = LogFileHelpers.logFileDirectory(testContext)

        var oldFile = File(directory, UUID.randomUUID().toString())
        oldFile.createNewFile()
        oldFile.setLastModified(1_000L)
        assert(oldFile.lastModified() == 1_000L)

        var newFile = File(directory, UUID.randomUUID().toString())
        newFile.createNewFile()
        newFile.setLastModified(1_000_000L)
        assert(newFile.lastModified() == 1_000_000L)

        val files = LogFileHelpers.logFiles(testContext)
        assert(files.first().lastModified() < files.last().lastModified())
    }
}
