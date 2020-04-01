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
import org.wordpress.android.util.helpers.logfile.LogFileProvider

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.O_MR1])
class LogFileHelpersTest {
    lateinit var testProvider: LogFileProvider

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
    fun testThatLogFileDirectoryIsCreatedIfNotExists() {
        val directory = testProvider.getLogFileDirectory()
        assert(directory.exists())
    }

    @Test
    fun testThatLogFilesListsAllFiles() {
        val directory = testProvider.getLogFileDirectory()
        File(directory, UUID.randomUUID().toString()).createNewFile()
        Assert.assertEquals(testProvider.getLogFiles().count(), 1)
    }

    @Test
    fun testThatLogFilesSortsFilesWithMostRecentFirst() {
        val directory = testProvider.getLogFileDirectory()

        var oldFile = File(directory, UUID.randomUUID().toString())
        oldFile.createNewFile()
        oldFile.setLastModified(1_000L)
        assert(oldFile.lastModified() == 1_000L)

        var newFile = File(directory, UUID.randomUUID().toString())
        newFile.createNewFile()
        newFile.setLastModified(1_000_000L)
        assert(newFile.lastModified() == 1_000_000L)

        val files = testProvider.getLogFiles()
        assert(files.first().lastModified() < files.last().lastModified())
    }
}
