package org.wordpress.android.usecase.social

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.PublicizeTableWrapper
import org.wordpress.android.models.PublicizeConnection
import org.wordpress.android.models.PublicizeService
import org.wordpress.android.ui.publicize.services.PublicizeUpdateServicesV2
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class GetPublicizeConnectionsForUserUseCaseTest : BaseUnitTest() {
    private val publicizeTableWrapper: PublicizeTableWrapper = mock()
    private val publicizeUpdateServicesV2: PublicizeUpdateServicesV2 = mock()

    private val classToTest = GetPublicizeConnectionsForUserUseCase(
        ioDispatcher = testDispatcher(),
        publicizeTableWrapper = publicizeTableWrapper,
        publicizeUpdateServicesV2 = publicizeUpdateServicesV2,
    )

    @Test
    fun `Should return publicize connection if connection user ID matches parameter user ID`() = test {
        val serviceId = "service"
        val siteId = 123L
        val currentUserId = 456L
        val publicizeConnection = PublicizeConnection().apply {
            userId = currentUserId.toInt()
            connectionId = 123
            service = serviceId
            label = "label"
            isShared = false
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
                    publicizeConnection,
                )
            )
        val expected = listOf(
            publicizeConnection,
        )
        val actual = classToTest.execute(
            siteId = siteId,
            userId = currentUserId,
            shouldForceUpdate = false,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return zero connections if all publicize services are UNSUPPORTED`() = test {
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
                    id = serviceId
                    status = PublicizeService.Status.UNSUPPORTED
                }
            ))
        whenever(publicizeTableWrapper.getConnectionsForSite(siteId))
            .thenReturn(
                listOf(
                    publicizeConnection1,
                    publicizeConnection2
                )
            )
        val expected = emptyList<PublicizeConnection>()
        val actual = classToTest.execute(
            siteId = siteId,
            userId = currentUserId,
            shouldForceUpdate = false,
        )
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return connection if isShared is true`() = test {
        val serviceId = "service"
        val siteId = 123L
        val currentUserId = 456L
        val publicizeConnection = PublicizeConnection().apply {
            userId = 1
            connectionId = 2
            service = serviceId
            label = "label"
            isShared = true
        }
        whenever(publicizeTableWrapper.getServiceList())
            .thenReturn(listOf(
                PublicizeService().apply {
                    id = serviceId
                    status = PublicizeService.Status.OK
                }
            ))
        whenever(publicizeTableWrapper.getConnectionsForSite(siteId))
            .thenReturn(
                listOf(
                    publicizeConnection,
                )
            )
        val expected = listOf(publicizeConnection)
        val actual = classToTest.execute(
            siteId = siteId,
            userId = currentUserId,
            shouldForceUpdate = false,
        )
        assertEquals(expected, actual)
    }
}
