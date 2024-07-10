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
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.config.PostConflictResolutionFeatureConfig

@ExperimentalCoroutinesApi
class PostConflictResolutionFeatureUtilsTest : BaseUnitTest() {
    @Mock
    lateinit var mPostConflictResolutionFeatureConfig: PostConflictResolutionFeatureConfig

    @Mock
    lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    private val site: SiteModel = mock()
    private val post: PostModel = mock()

    private lateinit var mPostConflictResolutionFeatureUtils: PostConflictResolutionFeatureUtils

    @Before
    fun setUp() {
        mPostConflictResolutionFeatureUtils = PostConflictResolutionFeatureUtils(
            mPostConflictResolutionFeatureConfig,
            dateTimeUtilsWrapper
        )
    }

    @Test
    fun `given feature is enabled, when request for payload, then shouldSkipConflictResolution to false`() {
        whenever(mPostConflictResolutionFeatureConfig.isEnabled()).thenReturn(true)
        val remotePostPayload = RemotePostPayload(post, site)

        val result = mPostConflictResolutionFeatureUtils.getRemotePostPayloadForPush(remotePostPayload)

        assertThat(result.shouldSkipConflictResolutionCheck).isFalse
    }

    @Test
    fun `given feature is disabled, when request for payload, then sets shouldSkipConflictResolution to true`() {
        whenever(mPostConflictResolutionFeatureConfig.isEnabled()).thenReturn(false)
        val remotePostPayload = RemotePostPayload(post, site)

        val result = mPostConflictResolutionFeatureUtils.getRemotePostPayloadForPush(remotePostPayload)

        assertThat(result.shouldSkipConflictResolutionCheck).isTrue
    }
}
