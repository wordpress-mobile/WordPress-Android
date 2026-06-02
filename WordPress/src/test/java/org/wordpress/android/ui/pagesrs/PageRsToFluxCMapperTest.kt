package org.wordpress.android.ui.pagesrs

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.postsrs.PostRsToFluxCMapper
import uniffi.wp_api.AnyPostWithEditContext

class PageRsToFluxCMapperTest {
    private val postMapper: PostRsToFluxCMapper = mock()
    private val mapper = PageRsToFluxCMapper(postMapper)

    @Test
    fun `map flips isPage to true on the delegated PostModel`() = runTest {
        val site = SiteModel().apply { id = 1 }
        val rsPage: AnyPostWithEditContext = mock()
        val postModel = PostModel().apply { setIsPage(false) }
        whenever(postMapper.map(any(), eq(site))).thenReturn(postModel)

        val result = mapper.map(rsPage, site)

        assertThat(result.isPage).isTrue()
        assertThat(result).isSameAs(postModel)
    }

    @Test
    fun `map preserves the other fields set by the delegated mapper`() = runTest {
        val site = SiteModel().apply { id = 2 }
        val rsPage: AnyPostWithEditContext = mock()
        val postModel = PostModel().apply {
            setRemotePostId(42L)
            setTitle("Hello pages")
            setIsPage(false)
        }
        whenever(postMapper.map(any(), eq(site))).thenReturn(postModel)

        val result = mapper.map(rsPage, site)

        assertThat(result.isPage).isTrue()
        assertThat(result.remotePostId).isEqualTo(42L)
        assertThat(result.title).isEqualTo("Hello pages")
    }
}
