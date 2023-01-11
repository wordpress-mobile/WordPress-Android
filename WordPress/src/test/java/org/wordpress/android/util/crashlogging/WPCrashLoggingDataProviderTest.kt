package org.wordpress.android.util.crashlogging

import android.content.SharedPreferences
import com.automattic.android.tracks.crashlogging.EventLevel.DEBUG
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.EncryptedLogging
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.LogFileProviderWrapper
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider.Companion.EVENT_BUS_EXCEPTION
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider.Companion.EVENT_BUS_INVOKING_SUBSCRIBER_FAILED_ERROR
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider.Companion.EVENT_BUS_MODULE
import org.wordpress.android.util.crashlogging.WPCrashLoggingDataProvider.Companion.EXTRA_UUID
import org.wordpress.android.viewmodel.ResourceProvider
import java.io.File
import java.util.Locale

@RunWith(MockitoJUnitRunner::class)
class WPCrashLoggingDataProviderTest {
    lateinit var sut: WPCrashLoggingDataProvider

    private val mockedFile: File = mock { on { exists() } doReturn true }

    private val logFileProvider: LogFileProviderWrapper = mock {
        on { getLogFiles() } doReturn listOf(mockedFile)
    }

    private val encryptedLogging: EncryptedLogging = mock {
        on { encryptAndUploadLogFile(any(), any()) } doReturn TEST_UUID
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.pref_key_send_crash) } doReturn SEND_CRASH_SAMPLE_KEY
    }

    private val accountStore: AccountStore = mock()
    private val localeManager: LocaleManagerWrapper = mock()
    private val buildConfig: BuildConfigWrapper = mock()
    private val sharedPreferences: SharedPreferences = mock()

    @Before
    fun setUp() {
        sut = WPCrashLoggingDataProvider(
            sharedPreferences,
            resourceProvider,
            accountStore,
            localeManager,
            encryptedLogging,
            logFileProvider,
            buildConfig
        )
    }

    @Test
    fun `should contain encrypted logs key for extra known keys`() {
        assertThat(sut.extraKnownKeys()).contains(EXTRA_UUID)
    }

    @Test
    fun `should correctly reads and maps user`() {
        whenever(accountStore.account).thenReturn(TEST_ACCOUNT)

        val user = sut.userProvider()

        assertSoftly { softly ->
            softly.assertThat(user?.username).isEqualTo(TEST_ACCOUNT.userName)
            softly.assertThat(user?.email).isEqualTo(TEST_ACCOUNT.email)
            softly.assertThat(user?.userID).isEqualTo(TEST_ACCOUNT.userId.toString())
        }
    }

    @Test
    fun `should not provide user if user does not exist`() {
        whenever(accountStore.account).thenReturn(null)

        val user = sut.userProvider()

        assertThat(user).isNull()
    }

    @Test
    fun `should provide empty application context`() {
        assertThat(sut.applicationContextProvider()).isEmpty()
    }

    @Test
    fun `should not provide encrypted logging uuid to extras if uuid is already applied`() {
        sut.provideExtrasForEvent(currentExtras = mapOf(EXTRA_UUID to TEST_UUID), eventLevel = DEBUG)

        verify(logFileProvider, never()).getLogFiles()
    }

    @Test
    fun `should provide encrypted logging uuid to extras if uuid is not applied`() {
        val extras = sut.provideExtrasForEvent(currentExtras = emptyMap(), eventLevel = DEBUG)

        verify(encryptedLogging).encryptAndUploadLogFile(eq(mockedFile), any())
        assertThat(extras).containsAllEntriesOf(mapOf(EXTRA_UUID to TEST_UUID))
    }

    @Test
    fun `should not provide encrypted logging uuid to extras if log file is not available`() {
        whenever(logFileProvider.getLogFiles()).thenReturn(emptyList())

        val extras = sut.provideExtrasForEvent(currentExtras = emptyMap(), eventLevel = DEBUG)

        verify(logFileProvider).getLogFiles()
        verify(encryptedLogging, never()).encryptAndUploadLogFile(any(), any())
        assertThat(extras).isEmpty()
    }

    @Test
    fun `should drop wrapping exception for eventbus exceptions`() {
        assertThat(
            sut.shouldDropWrappingException(
                module = EVENT_BUS_MODULE,
                type = EVENT_BUS_EXCEPTION,
                value = EVENT_BUS_INVOKING_SUBSCRIBER_FAILED_ERROR
            )
        ).isTrue
    }

    @Test
    fun `should provide locale from locale manager`() {
        val testLocale = Locale.US
        whenever(localeManager.getLocale()).thenReturn(testLocale)

        assertThat(sut.locale).isEqualTo(testLocale)
    }

    @Test
    fun `should disable crash logging for debug builds`() {
        whenever(buildConfig.isDebug()).thenReturn(true)

        assertThat(sut.crashLoggingEnabled()).isFalse
    }

    @Test
    fun `should disable crash logging if user has opt out in release`() {
        whenever(buildConfig.isDebug()).thenReturn(false)
        whenever(sharedPreferences.getBoolean(eq(SEND_CRASH_SAMPLE_KEY), any())).thenReturn(false)

        assertThat(sut.crashLoggingEnabled()).isFalse
    }

    @Test
    fun `should enable crash logging if user has not opt out in release`() {
        whenever(buildConfig.isDebug()).thenReturn(false)
        whenever(sharedPreferences.getBoolean(eq(SEND_CRASH_SAMPLE_KEY), any())).thenReturn(true)

        assertThat(sut.crashLoggingEnabled()).isTrue
    }

    companion object {
        val TEST_ACCOUNT = AccountModel().apply {
            userId = 123L
            email = "mail@a8c.com"
            userName = "username"
        }

        const val TEST_UUID = "test uuid"
        const val SEND_CRASH_SAMPLE_KEY = "send_crash"
    }
}
