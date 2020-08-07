package org.wordpress.android.fluxc.list.post

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import org.wordpress.android.fluxc.list.ListDescriptorUnitTestCase
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.AuthorFilter
import org.wordpress.android.fluxc.model.list.ListOrder.ASC
import org.wordpress.android.fluxc.model.list.ListOrder.DESC
import org.wordpress.android.fluxc.model.list.PostListDescriptor
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.model.list.PostListOrderBy.DATE
import org.wordpress.android.fluxc.model.list.PostListOrderBy.ID
import org.wordpress.android.fluxc.model.post.PostStatus

private typealias PostListDescriptorTestCase = ListDescriptorUnitTestCase<PostListDescriptor>

@RunWith(Parameterized::class)
internal class PostListDescriptorTest(
    private val testCase: PostListDescriptorTestCase
) {
    companion object {
        @JvmStatic
        @Parameters
        fun testCases(): List<PostListDescriptorTestCase> {
            val mockSite = mock<SiteModel>()
            val mockSite2 = mock<SiteModel>()
            whenever(mockSite.id).thenReturn(LIST_DESCRIPTOR_TEST_FIRST_MOCK_SITE_LOCAL_SITE_ID)
            whenever(mockSite2.id).thenReturn(LIST_DESCRIPTOR_TEST_SECOND_MOCK_SITE_LOCAL_SITE_ID)
            return listOf(
                    // PostListDescriptorForRestSite - PostListDescriptorForXmlRpcSite
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different descriptor types have different type identifiers",
                            uniqueIdentifierReason = "Different descriptor types have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(site = mockSite),
                            // We need to use a different site because the same site can't be both rest and xml-rpc
                            descriptor2 = PostListDescriptorForXmlRpcSite(site = mockSite2),
                            shouldHaveSameTypeIdentifier = false,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Same site
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Same sites should have same type identifier",
                            uniqueIdentifierReason = "Same sites should have same unique identifier",
                            descriptor1 = PostListDescriptorForRestSite(site = mockSite),
                            descriptor2 = PostListDescriptorForRestSite(site = mockSite),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = true
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Same sites should have same type identifier",
                            uniqueIdentifierReason = "Same sites should have same unique identifier",
                            descriptor1 = PostListDescriptorForXmlRpcSite(site = mockSite),
                            descriptor2 = PostListDescriptorForXmlRpcSite(site = mockSite),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = true
                    ),
                    // Different site
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different sites should have different type identifiers",
                            uniqueIdentifierReason = "Different sites should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(site = mockSite),
                            descriptor2 = PostListDescriptorForRestSite(site = mockSite2),
                            shouldHaveSameTypeIdentifier = false,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different sites should have different type identifiers",
                            uniqueIdentifierReason = "Different sites should have different unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(site = mockSite),
                            descriptor2 = PostListDescriptorForXmlRpcSite(site = mockSite2),
                            shouldHaveSameTypeIdentifier = false,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different status list
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different status lists should have same type identifiers",
                            uniqueIdentifierReason = "Different status lists should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    statusList = listOf(PostStatus.PUBLISHED)
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    statusList = listOf(PostStatus.DRAFT)
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different status lists should have same type identifiers",
                            uniqueIdentifierReason = "Different status lists should have different unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    statusList = listOf(PostStatus.PUBLISHED)
                            ),
                            descriptor2 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    statusList = listOf(PostStatus.DRAFT)
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different order
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different order should have same type identifiers",
                            uniqueIdentifierReason = "Different order should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    order = ASC
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    order = DESC
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different order should have same type identifiers",
                            uniqueIdentifierReason = "Different order should have different unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    order = ASC
                            ),
                            descriptor2 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    order = DESC
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different order by
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different order by should have same type identifiers",
                            uniqueIdentifierReason = "Different order by should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    orderBy = DATE
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    orderBy = ID
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different order by should have same type identifiers",
                            uniqueIdentifierReason = "Different order by should have different unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    orderBy = DATE
                            ),
                            descriptor2 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    orderBy = ID
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different search query
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different search query should have same type identifiers",
                            uniqueIdentifierReason = "Different search query should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    searchQuery = LIST_DESCRIPTOR_TEST_QUERY_1
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    searchQuery = LIST_DESCRIPTOR_TEST_QUERY_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different search query should have same type identifiers",
                            uniqueIdentifierReason = "Different search query should have different unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    searchQuery = LIST_DESCRIPTOR_TEST_QUERY_1
                            ),
                            descriptor2 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    searchQuery = LIST_DESCRIPTOR_TEST_QUERY_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    ),
                    // Different list config
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different list configs should have same type identifiers",
                            uniqueIdentifierReason = "Different list configs should have same unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = true
                    ),
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different list configs should have same type identifiers",
                            uniqueIdentifierReason = "Different list configs should have same unique identifiers",
                            descriptor1 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_1
                            ),
                            descriptor2 = PostListDescriptorForXmlRpcSite(
                                    mockSite,
                                    config = LIST_DESCRIPTOR_TEST_LIST_CONFIG_2
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = true
                    ),
                    // Different author which is only available for REST sites
                    PostListDescriptorTestCase(
                            typeIdentifierReason = "Different author should have same type identifiers",
                            uniqueIdentifierReason = "Different author should have different unique identifiers",
                            descriptor1 = PostListDescriptorForRestSite(
                                    mockSite,
                                    author = AuthorFilter.Everyone
                            ),
                            descriptor2 = PostListDescriptorForRestSite(
                                    mockSite,
                                    author = AuthorFilter.SpecificAuthor(1337)
                            ),
                            shouldHaveSameTypeIdentifier = true,
                            shouldHaveSameUniqueIdentifier = false
                    )
            )
        }
    }

    @Test
    fun `test type identifier`() {
        testCase.testTypeIdentifier()
    }

    @Test
    fun `test unique identifier`() {
        testCase.testUniqueIdentifier()
    }
}
