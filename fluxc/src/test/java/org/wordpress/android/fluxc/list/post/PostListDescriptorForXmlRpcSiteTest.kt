package org.wordpress.android.fluxc.list.post

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.ListOrder
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.list.PostListOrderBy.ID
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED

@RunWith(MockitoJUnitRunner::class)
class PostListDescriptorForXmlRpcSiteTest {
    private val mockSite = mock<SiteModel>()

    @Before
    fun setup() {
        whenever(mockSite.id).thenReturn(LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID)
    }

    @Test
    fun `type and unique identifiers of same descriptors should be the same`() {
        val descriptor1 = PostListDescriptorForXmlRpcSite(mockSite)
        val descriptor2 = PostListDescriptorForXmlRpcSite(mockSite)
        assertSameTypeIdentifiers(descriptor1, descriptor2)
        assertSameUniqueIdentifiers(descriptor1, descriptor2)
    }

    // Different sites

    @Test
    fun `type identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
        assertDifferentTypeIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite2)
        )
    }

    @Test
    fun `unique identifier of different site descriptors should be different`() {
        val mockSite2 = mock<SiteModel>()
        whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite2)
        )
    }

    // Different status list

    @Test
    fun `type identifier of different status list descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, statusList = listOf(PUBLISHED)),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, statusList = listOf(DRAFT))
        )
    }

    @Test
    fun `unique identifier of different status list descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, statusList = listOf(PUBLISHED)),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, statusList = listOf(DRAFT))
        )
    }

    // Different order

    @Test
    fun `type identifier of different order descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, order = ListOrder.ASC),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, order = ListOrder.DESC)
        )
    }

    @Test
    fun `unique identifier of different order descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, order = ListOrder.ASC),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, order = ListOrder.DESC)
        )
    }

    // Different order by

    @Test
    fun `type identifier of different order by descriptors should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, orderBy = DATE),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, orderBy = ID)
        )
    }

    @Test
    fun `unique identifier of different order by descriptors should be different`() {
        assertDifferentUniqueIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, orderBy = DATE),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, orderBy = ID)
        )
    }

    // Different config

    @Test
    fun `type identifier of different configs should be the same`() {
        assertSameTypeIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2)
        )
    }

    @Test
    fun `unique identifier of different configs should be the same`() {
        assertSameUniqueIdentifiers(
                descriptor1 = PostListDescriptorForXmlRpcSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1),
                descriptor2 = PostListDescriptorForXmlRpcSite(mockSite, config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2)
        )
    }
}
