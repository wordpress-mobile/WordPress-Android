package org.wordpress.android.fluxc.list.post

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListConfig
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.list.PostListOrderBy.ID
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED

private const val FIRST_MOCK_SITE_LOCAL_SITE_ID = 1
private const val SECOND_MOCK_SITE_LOCAL_SITE_ID = 2
private val LIST_CONFIG_1 = ListConfig(
        networkPageSize = 10,
        initialLoadSize = 10,
        dbPageSize = 10,
        prefetchDistance = 10
)
private val LIST_CONFIG_2 = ListConfig(
        networkPageSize = 20,
        initialLoadSize = 20,
        dbPageSize = 20,
        prefetchDistance = 20
)

@RunWith(MockitoJUnitRunner::class)
class PostListDescriptorRestSiteTest {
    private val mockSite = mock<SiteModel>()

    @Before
    fun setup() {
        whenever(mockSite.id).thenReturn(FIRST_MOCK_SITE_LOCAL_SITE_ID)
    }

    @Test
    fun `type and unique identifiers of same descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite)
        val descriptor2 = PostListDescriptorForRestSite(mockSite)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
        assertThat(descriptor1.uniqueIdentifier, equalTo(descriptor2.uniqueIdentifier))
    }

    // Different sites

    @Test
    fun `type identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(SECOND_MOCK_SITE_LOCAL_SITE_ID)
        val descriptor1 = PostListDescriptorForRestSite(mockSite)
        val descriptor2 = PostListDescriptorForRestSite(mockSite2)
        assertThat(descriptor1.typeIdentifier, not(equalTo(descriptor2.typeIdentifier)))
    }

    @Test
    fun `unique identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(SECOND_MOCK_SITE_LOCAL_SITE_ID)
        val descriptor1 = PostListDescriptorForRestSite(mockSite)
        val descriptor2 = PostListDescriptorForRestSite(mockSite2)
        assertThat(descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
    }

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

    // Different order by

    @Test
    fun `type identifier of different order by descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, orderBy = DATE)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, orderBy = ID)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
    }

    @Test
    fun `unique identifier of different order by descriptors should be different`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, orderBy = DATE)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, orderBy = ID)
        assertThat(descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
    }

    // Different search query

    @Test
    fun `type identifier of different search descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, searchQuery = "any query")
        val descriptor2 = PostListDescriptorForRestSite(mockSite, searchQuery = null)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
    }

    @Test
    fun `unique identifier of different search descriptors should be different`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, searchQuery = "any query")
        val descriptor2 = PostListDescriptorForRestSite(mockSite, searchQuery = null)
        assertThat(descriptor1.uniqueIdentifier, not(equalTo(descriptor2.uniqueIdentifier)))
    }

    // Different config

    @Test
    fun `type identifier of different configs should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, config = LIST_CONFIG_1)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, config = LIST_CONFIG_2)
        assertThat(descriptor1.typeIdentifier, equalTo(descriptor2.typeIdentifier))
    }

    @Test
    fun `unique identifier of different configs should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite, config = LIST_CONFIG_1)
        val descriptor2 = PostListDescriptorForRestSite(mockSite, config = LIST_CONFIG_2)
        assertThat(descriptor1.uniqueIdentifier, equalTo(descriptor2.uniqueIdentifier))
    }
}
