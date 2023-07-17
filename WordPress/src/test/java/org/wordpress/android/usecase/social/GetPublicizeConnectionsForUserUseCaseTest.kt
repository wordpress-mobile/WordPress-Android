package org.wordpress.android.usecase.social

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class GetPublicizeConnectionsForUserUseCaseTest : BaseUnitTest() {
    private val publicizeTableWrapper: PublicizeTableWrapper = mock()

    private val classToTest = GetPublicizeConnectionsForUserUseCase(
        ioDispatcher = testDispatcher(),
        publicizeTableWrapper = publicizeTableWrapper,
    )

    @Test
    fun `Should return publicize connections for user`() = test {
        val serviceId = "service"
        val siteId = 123L
        val currentUserId = 456L
        val publicizeConnection1 = PublicizeConnection().apply {
            userId = currentUserId.toInt()
            connectionId = 123
            service = serviceId
            label = "label"
            isShared = false
        }
        val publicizeConnection2 = PublicizeConnection().apply {
            userId = currentUserId.toInt()
            connectionId = 123
            service = serviceId
            label = "label"
            isShared = true
        }
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(
                PublicizeService().apply {
                    id = "1"
                    status = PublicizeService.Status.UNSUPPORTED
                },
                PublicizeService().apply {
                    id = "2"
                    status = PublicizeService.Status.UNSUPPORTED
                },
                PublicizeService().apply {
                    id = serviceId
                    status = PublicizeService.Status.OK
                }
            ))
        whenever(publicizeTableWrapper.getConnectionsForSite(siteId))
            .thenReturn(
                listOf(
                    PublicizeConnection().apply {
                        userId = 123
                        connectionId = 123
                        service = "123"
                        label = "label"
                        isShared = false
                    },
                    PublicizeConnection().apply {
                        userId = 123
                        connectionId = 123
                        service = serviceId
                        label = "label"
                        isShared = false
                    },
                    publicizeConnection1,
                    publicizeConnection2
                )
            )
        val expected = listOf(publicizeConnection1, publicizeConnection2)
        val actual = classToTest.execute(siteId, currentUserId)
        assertEquals(expected, actual)
    }
}
