package org.wordpress.android.ui.posts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.util.config.SyncPublishingFeatureConfig

@ExperimentalCoroutinesApi
class SyncPublishingFeatureUtilsTest : BaseUnitTest() {
    @Mock
    lateinit var syncPublishingFeatureConfig: SyncPublishingFeatureConfig

    private val site: SiteModel = mock()
    private val post: PostModel = mock()

    private lateinit var syncPublishingFeatureUtils: SyncPublishingFeatureUtils

    @Before
    fun setUp() {
        syncPublishingFeatureUtils = SyncPublishingFeatureUtils(syncPublishingFeatureConfig)
    }

    @Test
    fun `given feature is enabled, when request for payload, then shouldSkipConflictResolution to false`() {
        whenever(syncPublishingFeatureConfig.isEnabled()).thenReturn(true)
        val remotePostPayload = RemotePostPayload(post, site)

        val result = syncPublishingFeatureUtils.getRemotePostPayloadForPush(remotePostPayload)

        assertThat(result.shouldSkipConflictResolutionCheck).isFalse
    }

    @Test
    fun `given feature is disabled, when request for payload, then sets shouldSkipConflictResolution to true`() {
        whenever(syncPublishingFeatureConfig.isEnabled()).thenReturn(false)
        val remotePostPayload = RemotePostPayload(post, site)

        val result = syncPublishingFeatureUtils.getRemotePostPayloadForPush(remotePostPayload)

        assertThat(result.shouldSkipConflictResolutionCheck).isTrue
    }
}
