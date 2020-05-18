package org.wordpress.android.fluxc.model

import org.assertj.core.api.AssertionsForInterfaceTypes.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteHomepageSettings.StaticPage
import org.wordpress.android.fluxc.model.SiteHomepageSettings.Posts
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteHomepageRestClient.UpdateHomepageResponse

@RunWith(MockitoJUnitRunner::class)
class SiteHomepageSettingsMapperTest {
    private val mapper = SiteHomepageSettingsMapper()

    @Test
    fun `maps site to page homepage settings`() {
        val site = SiteModel()
        site.showOnFront = "page"
        val pageForPostsId: Long = 1
        site.pageForPosts = pageForPostsId
        val pageOnFrontId: Long = 2
        site.pageOnFront = pageOnFrontId
        val homepageSettings = mapper.map(site)

        assertThat((homepageSettings as StaticPage).pageForPostsId).isEqualTo(pageForPostsId)
        assertThat(homepageSettings.pageOnFrontId).isEqualTo(pageOnFrontId)
    }

    @Test
    fun `maps site to posts homepage settings`() {
        val site = SiteModel()
        site.showOnFront = "posts"
        val homepageSettings = mapper.map(site)

        assertThat(homepageSettings is Posts).isTrue()
    }

    @Test
    fun `returns null on unexpected type`() {
        val site = SiteModel()
        site.showOnFront = "unexpected"

        val homepageSettings = mapper.map(site)

        assertThat(homepageSettings).isNull()
    }

    @Test
    fun `maps response to page homepage settings`() {
        val pageForPostsId: Long = 1
        val pageOnFrontId: Long = 2
        val response = UpdateHomepageResponse(true, pageOnFrontId, pageForPostsId)

        val homepageSettings = mapper.map(response)

        assertThat((homepageSettings as StaticPage).pageForPostsId).isEqualTo(pageForPostsId)
        assertThat(homepageSettings.pageOnFrontId).isEqualTo(pageOnFrontId)
    }

    @Test
    fun `maps response to posts homepage settings`() {
        val response = UpdateHomepageResponse(false, null, null)

        val homepageSettings = mapper.map(response)

        assertThat(homepageSettings is Posts).isTrue()
    }
}
