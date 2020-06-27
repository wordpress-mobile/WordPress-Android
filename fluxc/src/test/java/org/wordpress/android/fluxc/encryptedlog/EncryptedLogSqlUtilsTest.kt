package org.wordpress.android.fluxc.encryptedlog

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.EncryptedLog
import org.wordpress.android.fluxc.model.EncryptedLogModel
import org.wordpress.android.fluxc.model.EncryptedLogUploadState
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import java.io.File
import java.time.temporal.ChronoUnit
import java.util.Date

private const val TEST_UUID = "TEST_UUID"
private const val TEST_FILE_PATH = "TEST_FILE_PATH"

@RunWith(RobolectricTestRunner::class)
class EncryptedLogSqlUtilsTest {
    private lateinit var sqlUtils: EncryptedLogSqlUtils

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(appContext, EncryptedLogModel::class.java)
        WellSql.init(config)
        config.reset()

        sqlUtils = EncryptedLogSqlUtils()
    }

    @Test
    fun testInsertEncryptedLog() {
        // Assert that there are no encrypted logs with the test uuid
        assertThat(getTestEncryptedLogFromDB()).isNull()

        // Insert an encrypted log with uuid
        val logToBeInserted = createTestEncryptedLog()
        sqlUtils.insertOrUpdateEncryptedLog(logToBeInserted)

        // Assert that the encrypted log from the DB is the same as the one we inserted
        val log = getTestEncryptedLogFromDB()
        assertThat(log).isEqualToComparingFieldByField(logToBeInserted)
    }

    @Test
    fun testUpdateEncryptedLog() {
        // Insert an initial encrypted log
        val initialLog = createTestEncryptedLog()
        sqlUtils.insertOrUpdateEncryptedLog(initialLog)
        assertThat(getTestEncryptedLogFromDB()).isEqualToComparingFieldByField(initialLog)

        // Create a copy of the encrypted log by changing its upload state (which will be the common usage)
        val newUploadState = EncryptedLogUploadState.NEEDS_UPLOAD
        val updatedLog = initialLog.copy(uploadState = newUploadState)
        sqlUtils.insertOrUpdateEncryptedLog(updatedLog)

        // Assert that the encrypted log in the DB is the one with the correct upload state
        val updatedLogFromDB = getTestEncryptedLogFromDB()
        assertThat(requireNotNull(updatedLogFromDB?.uploadState)).isEqualTo(newUploadState)
        // This verifies the expected state as well but separating the initial assertion is valuable to show intent
        assertThat(updatedLogFromDB).isEqualToComparingFieldByField(updatedLog)
    }

    @Test
    fun testDeleteEncryptedLog() {
        // Insert an initial encrypted log
        val initialLog = createTestEncryptedLog()
        sqlUtils.insertOrUpdateEncryptedLog(initialLog)
        assertThat(getTestEncryptedLogFromDB()).isEqualToComparingFieldByField(initialLog)

        // Delete the encrypted log
        sqlUtils.deleteEncryptedLog(TEST_UUID)

        // Assert that the encrypted log no longer exists
        assertThat(getTestEncryptedLogFromDB()).isNull()
    }

    private fun getTestEncryptedLogFromDB() = sqlUtils.getEncryptedLog(TEST_UUID)

    private fun createTestEncryptedLog(
        dateCreated: Date = Date(),
        uuid: String = TEST_UUID,
        filePath: String = TEST_FILE_PATH,
        uploadState: EncryptedLogUploadState = EncryptedLogUploadState.CREATED
    ) = EncryptedLog(
            // Bypass the annoying milliseconds comparison issue
            dateCreated = Date.from(dateCreated.toInstant().truncatedTo(ChronoUnit.SECONDS)),
            uuid = uuid,
            file = File(filePath),
            uploadState = uploadState
    )
}
