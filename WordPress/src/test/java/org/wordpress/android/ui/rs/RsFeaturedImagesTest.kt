package org.wordpress.android.ui.rs

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.rs.contentlist.ContentItemUiModel
import org.wordpress.android.ui.rs.contentlist.RsMenuAction
import org.wordpress.android.ui.rs.data.FeaturedImageUrls
import org.wordpress.android.ui.rs.data.RsSiteRestClient

@ExperimentalCoroutinesApi
class RsFeaturedImagesTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var restClient: RsSiteRestClient

    private val site = SiteModel()
    private val resolved = mutableListOf<Pair<String, Map<Long, FeaturedImageUrls>>>()

    @Before
    fun setUp() {
        resolved.clear()
    }

    private fun createFeaturedImages() = RsFeaturedImages<String>(
        scope = testScope(),
        restClient = restClient,
        onImagesResolved = { tab, images -> resolved.add(tab to images) },
        ioDispatcher = testDispatcher(),
    )

    private fun model(mediaId: Long, image: FeaturedImageUrls? = null) =
        ContentItemUiModel<RsMenuAction>(
            remoteId = mediaId * 10,
            title = "",
            excerpt = "",
            date = "",
            featuredImageId = mediaId,
            featuredImage = image,
        )

    private suspend fun answer(images: Map<Long, FeaturedImageUrls>) {
        whenever(restClient.fetchFeaturedImageUrls(anyOrNull(), any(), any(), any()))
            .doSuspendableAnswer { images }
    }

    @Test
    fun `only ids still lacking an image are looked up, once each`() = test {
        answer(mapOf(ONE to IMAGE))
        val featuredImages = createFeaturedImages()

        featuredImages.resolve(TAB, site, listOf(model(ONE), model(ONE), model(TWO, IMAGE), model(0L)))
        advanceUntilIdle()

        verify(restClient).fetchFeaturedImageUrls(eq(site), eq(listOf(ONE)), any(), any())
        assertThat(resolved).containsExactly(TAB to mapOf(ONE to IMAGE))
    }

    @Test
    fun `an id the lookup could not resolve stops its row waiting`() = test {
        answer(emptyMap())
        val featuredImages = createFeaturedImages()

        featuredImages.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        assertThat(featuredImages.withImage(model(ONE), emptyMap()).isFeaturedImageUnresolvable).isTrue
        assertThat(featuredImages.carryOver(model(ONE), null).isFeaturedImageUnresolvable).isTrue
    }

    @Test
    fun `an id that already failed is not looked up again until a refresh`() = test {
        answer(emptyMap())
        val featuredImages = createFeaturedImages()
        featuredImages.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        featuredImages.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        verify(restClient, times(1)).fetchFeaturedImageUrls(anyOrNull(), any(), any(), any())
    }

    @Test
    fun `a running lookup that covers the ids is not restarted`() = test {
        val gate = CompletableDeferred<Map<Long, FeaturedImageUrls>>()
        whenever(restClient.fetchFeaturedImageUrls(anyOrNull(), any(), any(), any()))
            .doSuspendableAnswer { gate.await() }
        val featuredImages = createFeaturedImages()
        featuredImages.resolve(TAB, site, listOf(model(ONE), model(TWO)))
        advanceUntilIdle()

        featuredImages.resolve(TAB, site, listOf(model(ONE)))
        gate.complete(mapOf(ONE to IMAGE))
        advanceUntilIdle()

        verify(restClient, times(1)).fetchFeaturedImageUrls(anyOrNull(), any(), any(), any())
        assertThat(resolved).containsExactly(TAB to mapOf(ONE to IMAGE))
    }

    @Test
    fun `invalidating forgets the failures so a refresh asks again`() = test {
        answer(emptyMap())
        val featuredImages = createFeaturedImages()
        featuredImages.resolve(TAB, site, listOf(model(ONE)))
        advanceUntilIdle()

        featuredImages.invalidateUnresolved()

        assertThat(featuredImages.carryOver(model(ONE), null).isFeaturedImageUnresolvable).isFalse
    }

    @Test
    fun `withImage returns the same model when there is nothing to change`() {
        val featuredImages = createFeaturedImages()
        val model = model(ONE)

        assertThat(featuredImages.withImage(model, mapOf(TWO to IMAGE))).isSameAs(model)
        assertThat(featuredImages.withImage(model, mapOf(ONE to IMAGE)).featuredImage).isEqualTo(IMAGE)
    }

    @Test
    fun `carryOver keeps an image only while the media id is unchanged`() {
        val featuredImages = createFeaturedImages()

        assertThat(featuredImages.carryOver(model(ONE), model(ONE, IMAGE)).featuredImage).isEqualTo(IMAGE)
        assertThat(featuredImages.carryOver(model(TWO), model(ONE, IMAGE)).featuredImage).isNull()
        assertThat(featuredImages.carryOver(model(0L), model(0L, IMAGE)).featuredImage).isNull()
    }

    private companion object {
        const val TAB = "tab"
        const val ONE = 1L
        const val TWO = 2L
        val IMAGE = FeaturedImageUrls(thumbnail = "thumb", hero = "hero")
    }
}
