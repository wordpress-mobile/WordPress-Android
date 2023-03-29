package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@RunWith(MockitoJUnitRunner::class)
class VideoPressBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private val uriWrapper = Mockito.mock(UriWrapper::class.java)
    private lateinit var processor: VideoPressBlockProcessor

    // Keys for each query in VideoPress URL.
    private val resizeToParentKey = "resizeToParent"
    private val coverKey = "cover"
    private val autoPlayKey = "autoPlay"
    private val controlsKey = "controls"
    private val loopKey = "loop"
    private val mutedKey = "muted"
    private val persistVolumeKey = "persistVolume"
    private val playsinlineKey = "playsinline"
    private val posterUrlKey = "posterUrl"
    private val preloadContentKey = "preloadContent"
    private val sbcKey = "sbc"
    private val sbpcKey = "sbpc"
    private val sblcKey = "sblc"
    private val useAverageColorKey = "useAverageColor"

    // Un-encoded values for each query in VideoPress URL.
    private val falseVal = "false"
    private val trueVal = "true"
    private val posterUrlVal = "https://test.files.wordpress.com/2022/02/265-5000x5000-1.jpeg"
    private val defaultPreloadContentVal = "metadata"
    private val changedPreloadContentVal = "none"
    private val sbcVal = "#abb8c3"
    private val sbpcVal = "#9b51e0"
    private val sblcVal = "#cf2e2e"

    private val urlKeys = listOf(
        resizeToParentKey,
        coverKey,
        autoPlayKey,
        controlsKey,
        loopKey,
        mutedKey,
        persistVolumeKey,
        playsinlineKey,
        posterUrlKey,
        preloadContentKey,
        sbcKey,
        sbpcKey,
        sblcKey,
        useAverageColorKey
    )
    private val urlValues = listOf(
        falseVal,
        trueVal,
        posterUrlVal,
        defaultPreloadContentVal,
        changedPreloadContentVal,
        sbcVal,
        sbpcVal,
        sblcVal
    )

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.videoPressGuid).thenReturn(TestContent.videoPressGuid)

        /*
        * As Uri.encode is part of an Android class, it cannot run locally in unit tests by default.
        * To workaround this, it has been replaced below with URLEncoder.encode, which works in a similar manner.
        * Note, we cannot currently use URLEncoder.encode in the main app as it only runs with API 33 or later.
        * We support a minimum of API 24.
        */
        for (key in urlKeys) {
            whenever(uriWrapper.encode(key)).thenReturn(URLEncoder.encode(key, StandardCharsets.UTF_8))
        }

        for (value in urlValues) {
            whenever(uriWrapper.encode(value)).thenReturn(URLEncoder.encode(value, StandardCharsets.UTF_8))
        }

        processor = VideoPressBlockProcessor(TestContent.localMediaId, mediaFile, uriWrapper)
    }

    @Test
    fun `processBlock replaces id and contents in VideoPress block with default attributes`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoPressBlockWithDefaultAttrs)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoPressBlockWithDefaultAttrs)
    }

    @Test
    fun `processBlock replaces id and contents in VideoPress block with different attributes to the default`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoPressBlockWithAttrs)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoPressBlockWithAttrs)
    }

    @Test
    fun `processBlock leaves Video block unchanged`() {
        val nonMatchingId = "123"
        val processor = VideoPressBlockProcessor(nonMatchingId, mediaFile)
        val processedBlock = processor.processBlock(TestContent.oldVideoBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.oldVideoBlock)
    }
}
