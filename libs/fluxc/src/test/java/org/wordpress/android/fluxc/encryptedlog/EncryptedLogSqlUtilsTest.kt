package org.wordpress.android.fluxc.encryptedlog

import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLog
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogModel
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.QUEUED
import org.wordpress.android.fluxc.model.encryptedlogging.EncryptedLogUploadState.UPLOADING
import org.wordpress.android.fluxc.persistence.EncryptedLogSqlUtils
import java.io.File
import java.time.temporal.ChronoUnit.SECONDS
import java.util.Date
import java.util.UUID
import kotlin.random.Random

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
    fun `test insert encrypted log`() {
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
    fun `test update encrypted log`() {
        // Insert an initial encrypted log
        val initialLog = createTestEncryptedLog()
        sqlUtils.insertOrUpdateEncryptedLog(initialLog)
        assertThat(getTestEncryptedLogFromDB()).isEqualToComparingFieldByField(initialLog)

        // Create a copy of the encrypted log by changing its upload state (which will be the common usage)
        val newUploadState = EncryptedLogUploadState.UPLOADING
        val updatedLog = initialLog.copy(uploadState = newUploadState)
        sqlUtils.insertOrUpdateEncryptedLog(updatedLog)

        // Assert that the encrypted log in the DB is the one with the correct upload state
        val updatedLogFromDB = getTestEncryptedLogFromDB()
        assertThat(requireNotNull(updatedLogFromDB?.uploadState)).isEqualTo(newUploadState)
        // This verifies the expected state as well but separating the initial assertion is valuable to show intent
        assertThat(updatedLogFromDB).isEqualToComparingFieldByField(updatedLog)
    }

    @Test
    fun `test delete encrypted log`() {
        // Insert an initial encrypted log
        val initialLog = createTestEncryptedLog()
        sqlUtils.insertOrUpdateEncryptedLog(initialLog)
        assertThat(getTestEncryptedLogFromDB()).isEqualToComparingFieldByField(initialLog)

        // Delete the encrypted log
        sqlUtils.deleteEncryptedLogs(listOf(initialLog))

        // Assert that the encrypted log no longer exists
        assertThat(getTestEncryptedLogFromDB()).isNull()
    }

    @Test
    fun `test uploading encrypted logs count for empty DB`() {
        assertThat(sqlUtils.getNumberOfUploadingEncryptedLogs()).isEqualTo(0)
    }

    @Test
    fun `test uploading encrypted logs for random number`() {
        Random.nextInt(100).let { numberOfLogs ->
            repeat(numberOfLogs) {
                sqlUtils.insertOrUpdateEncryptedLog(
                        createTestEncryptedLog(
                                uuid = UUID.randomUUID().toString(),
                                uploadState = UPLOADING
                        )
                )
            }
            assertThat(sqlUtils.getNumberOfUploadingEncryptedLogs()).isEqualTo(numberOfLogs.toLong())
        }
    }

    private fun getTestEncryptedLogFromDB() = sqlUtils.getEncryptedLog(TEST_UUID)

    private fun createTestEncryptedLog(
        uuid: String = TEST_UUID,
        filePath: String = TEST_FILE_PATH,
        dateCreated: Date = Date(),
        uploadState: EncryptedLogUploadState = QUEUED
    ) = EncryptedLog(
            // Bypass the annoying milliseconds comparison issue
            uuid = uuid,
            file = File(filePath),
            dateCreated = Date.from(dateCreated.toInstant().truncatedTo(SECONDS)),
            uploadState = uploadState
    )
}
