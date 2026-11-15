package org.wordpress.android.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaMetadataRetriever
import org.robolectric.shadows.util.DataSource
import org.wordpress.android.fluxc.model.SiteModel

/**
 * Covers [MediaUtilsWrapper.isProhibitedVideoDuration]: the five-minute limit only applies to
 * WordPress.com-hosted sites on the free plan, not to self-hosted sites that also report a free plan.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class)
class MediaUtilsWrapperTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val videoUri: Uri = Uri.parse("file:///tmp/long-video.mp4")
    private lateinit var wrapper: MediaUtilsWrapper

    @Before
    fun setUp() {
        wrapper = MediaUtilsWrapper(context)
        ShadowMediaMetadataRetriever.reset()
        ShadowMediaMetadataRetriever.addMetadata(
            DataSource.toDataSource(context, videoUri),
            MediaMetadataRetriever.METADATA_KEY_DURATION,
            SIX_MINUTES_MS.toString()
        )
    }

    @Test
    fun `prohibits long videos on free WordPressCom sites`() {
        val site = site(isWPCom = true, isFreePlan = true)

        assertThat(wrapper.isProhibitedVideoDuration(context, site, videoUri)).isTrue()
    }

    @Test
    fun `allows long videos on self-hosted sites that report a free plan`() {
        val site = site(isWPCom = false, isFreePlan = true)

        assertThat(wrapper.isProhibitedVideoDuration(context, site, videoUri)).isFalse()
    }

    @Test
    fun `allows long videos on paid WordPressCom sites`() {
        val site = site(isWPCom = true, isFreePlan = false)

        assertThat(wrapper.isProhibitedVideoDuration(context, site, videoUri)).isFalse()
    }

    @Test
    fun `allows long videos when VideoPress is active`() {
        val site = site(isWPCom = true, isFreePlan = true, activeModules = "videopress")

        assertThat(wrapper.isProhibitedVideoDuration(context, site, videoUri)).isFalse()
    }

    @Test
    fun `does not crash when the video duration cannot be read`() {
        ShadowMediaMetadataRetriever.addException(
            DataSource.toDataSource(context, videoUri),
            RuntimeException("cannot read metadata")
        )
        val site = site(isWPCom = true, isFreePlan = true)

        assertThat(wrapper.isProhibitedVideoDuration(context, site, videoUri)).isFalse()
    }

    private fun site(isWPCom: Boolean, isFreePlan: Boolean, activeModules: String = ""): SiteModel =
        SiteModel().apply {
            setIsWPCom(isWPCom)
            setHasFreePlan(isFreePlan)
            setActiveModules(activeModules)
        }

    companion object {
        private const val SIX_MINUTES_MS = 6 * 60 * 1000L
    }
}
