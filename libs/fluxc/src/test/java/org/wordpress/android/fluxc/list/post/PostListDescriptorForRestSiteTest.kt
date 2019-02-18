package org.wordpress.android.fluxc.list.post

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.list.PostListOrderBy.ID
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED

@RunWith(MockitoJUnitRunner::class)
class PostListDescriptorForRestSiteTest {
    private val mockSite = mock<SiteModel>()

    @Before
    fun setup() {
        whenever(mockSite.id).thenReturn(LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID)
    }

    @Test
    fun `type and unique identifiers of same descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForRestSite(mockSite)
        val descriptor2 = PostListDescriptorForRestSite(mockSite)
        assertSameTypeIdentifiers(descriptor1, descriptor2)
        assertSameUniqueIdentifiers(descriptor1, descriptor2)
    }

    // Different sites

    @Test
    fun `type identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
        assertDifferentTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite),
                descriptor2 = PostListDescriptorForRestSite(mockSite2)
        )
    }

    @Test
    fun `unique identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite),
                descriptor2 = PostListDescriptorForRestSite(mockSite2)
        )
    }

    // Different status list

    @Test
    fun `type identifier of different status list descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, statusList = listOf(PUBLISHED)),
                descriptor2 = PostListDescriptorForRestSite(mockSite, statusList = listOf(DRAFT))
        )
    }

    @Test
    fun `unique identifier of different status list descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, statusList = listOf(PUBLISHED)),
                descriptor2 = PostListDescriptorForRestSite(mockSite, statusList = listOf(DRAFT))
        )
    }

    // Different order

    @Test
    fun `type identifier of different order descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, order = ListOrder.ASC),
                descriptor2 = PostListDescriptorForRestSite(mockSite, order = ListOrder.DESC)
        )
    }

    @Test
    fun `unique identifier of different order descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, order = ListOrder.ASC),
                descriptor2 = PostListDescriptorForRestSite(mockSite, order = ListOrder.DESC)
        )
    }

    // Different order by

    @Test
    fun `type identifier of different order by descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, orderBy = DATE),
                descriptor2 = PostListDescriptorForRestSite(mockSite, orderBy = ID)
        )
    }

    @Test
    fun `unique identifier of different order by descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, orderBy = DATE),
                descriptor2 = PostListDescriptorForRestSite(mockSite, orderBy = ID)
        )
    }

    // Different search query

    @Test
    fun `type identifier of different search descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, searchQuery = LIST_DESCRIPTOR_TEST_QUERY_1),
                descriptor2 = PostListDescriptorForRestSite(mockSite, searchQuery = LIST_DESCRIPTOR_TEST_QUERY_2)
        )
    }

    @Test
    fun `unique identifier of different search descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, searchQuery = LIST_DESCRIPTOR_TEST_QUERY_1),
                descriptor2 = PostListDescriptorForRestSite(mockSite, searchQuery = LIST_DESCRIPTOR_TEST_QUERY_2)
        )
    }

    // Different config

    @Test
    fun `type identifier of different configs should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1),
                descriptor2 = PostListDescriptorForRestSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2)
        )
    }

    @Test
    fun `unique identifier of different configs should be the same`() {
        assertSameUniqueIdentifiers(
                descriptor1 = PostListDescriptorForRestSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1),
                descriptor2 = PostListDescriptorForRestSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2)
        )
    }
}
