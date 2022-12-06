package org.wordpress.android.localcontentmigration

import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.store.mobile.JetpackMigrationStore
import org.wordpress.android.fluxc.store.mobile.MigrationCompleteFetchedPayload
import org.wordpress.android.test

@InternalCoroutinesApi
class MigrationEmailHelperTest : BaseUnitTest() {
    private val jetpackMigrationStore: JetpackMigrationStore = mock()
    private val migrationAnalyticsTracker: ContentMigrationAnalyticsTracker = mock()

    private val classToTest = MigrationEmailHelper(
            jetpackMigrationStore,
            migrationAnalyticsTracker,
            TEST_DISPATCHER,
    )

    @Test
    fun `Should call migrationComplete when notifyMigrationComplete is called`() = test {
        classToTest.notifyMigrationComplete()
        verify(jetpackMigrationStore).migrationComplete()
    }

    @Test
    fun `Should call trackMigrationEmailSuccess if migrationComplete returns Success`() = test {
        whenever(jetpackMigrationStore.migrationComplete()).thenReturn(MigrationCompleteFetchedPayload.Success)
        classToTest.notifyMigrationComplete()
        verify(migrationAnalyticsTracker).trackMigrationEmailSuccess()
    }

    @Test
    fun `Should call trackMigrationEmailFailed if migrationComplete returns Error`() = test {
        val error = MigrationCompleteFetchedPayload.Error(BaseNetworkError(GenericErrorType.NETWORK_ERROR))
        whenever(jetpackMigrationStore.migrationComplete()).thenReturn(error)
        classToTest.notifyMigrationComplete()
        // We use `any()` here because `EmailError` is not a data class, so verifying the instance would not work.
        verify(migrationAnalyticsTracker).trackMigrationEmailFailed(any())
    }
}
