package org.wordpress.android.fluxc.list

import com.nhaarman.mockitokotlin2.mock
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED

@RunWith(MockitoJUnitRunner::class)
class PostListDescriptorRestSiteTest {
    private val mockSite = mock<SiteModel>()

    @Test
    fun `type and unique identifiers of same descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite)
        val descriptor2 = PostListDescriptorForRestSite(mockSite)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
        assertThat(descriptor1.uniqueIdentifier, equalTo(descriptor2.uniqueIdentifier))
    }

    // TODO: Test unique and type identifiers for different sites

    // Different status list

    @Test
    fun `type identifier of different status list descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, statusList = listOf(PUBLISHED))
        val descriptor2 = PostListDescriptorForRestSite(mockSite, statusList = listOf(DRAFT))
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
    }

    @Test
    fun `unique identifier of different status list descriptors should be different`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, statusList = listOf(PUBLISHED))
        val descriptor2 = PostListDescriptorForRestSite(mockSite, statusList = listOf(DRAFT))
        assertThat(descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
    }

    // Different order

    @Test
    fun `type identifier of different order descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, order = ListOrder.ASC)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, order = ListOrder.DESC)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
    }

    @Test
    fun `unique identifier of different order descriptors should be different`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, order = ListOrder.ASC)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, order = ListOrder.DESC)
        assertThat(descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
    }

    // TODO: Test unique and type identifiers for different order by

    // TODO: Test unique and type identifiers for different search query

    // TODO: Test unique and type identifiers for different config
}
