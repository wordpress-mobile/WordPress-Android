package org.wordpress.android.ui.posts.editor

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.wordpress.android.BaseUnitTest
import java.io.File

private const val TEST_OUTPUT_FOLDER = "outputFolder"
private const val TEST_OUTPUT_FILE_NAME = "outputFile"
private const val DURATION = 24 * 60 * 60 * 1000.toLong()

@ExperimentalCoroutinesApi
class ImageEditorFileUtilsTest : BaseUnitTest() {
    @Rule
    @JvmField val temporaryFolder = TemporaryFolder()

    // Class under test
    private lateinit var fileUtils: ImageEditorFileUtils

    @Before
    fun setUp() {
        fileUtils = ImageEditorFileUtils()
    }

    @Test
    fun `if file inside directory is modified within given duration, it is not deleted`() {
        // Arrange
        val directory: File = temporaryFolder.newFolder(TEST_OUTPUT_FOLDER)
        val directoryPath: String = directory.path

        val outputFile = File(directory, TEST_OUTPUT_FILE_NAME)
        outputFile.createNewFile()

        // Act
        test {
            fileUtils.deleteFilesOlderThanDurationFromDirectory(directoryPath, DURATION)
        }

        // Assert
        assertThat(outputFile.exists()).isTrue()
    }

    @Test
    fun `if file inside directory is modified on or after given duration, it is deleted`() {
        // Arrange
        val directory: File = temporaryFolder.newFolder(TEST_OUTPUT_FOLDER)
        val directoryPath: String = directory.path

        val outputFile = File(directory, TEST_OUTPUT_FILE_NAME)
        outputFile.createNewFile()
        outputFile.setLastModified(System.currentTimeMillis() - DURATION)

        // Act
        test {
            fileUtils.deleteFilesOlderThanDurationFromDirectory(directoryPath, DURATION)
        }

        // Assert
        assertThat(outputFile.exists()).isFalse()
    }

    @After
    fun tearDown() {
        temporaryFolder.delete()
    }
}
