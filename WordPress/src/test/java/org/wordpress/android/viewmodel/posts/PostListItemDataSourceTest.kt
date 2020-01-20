package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.PostSummary
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.PUBLISHED
import org.wordpress.android.ui.posts.PostListType.SCHEDULED
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.posts.PostListType.TRASHED
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.EndListIndicatorIdentifier
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.LocalPostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.RemotePostId
import org.wordpress.android.viewmodel.posts.PostListItemIdentifier.SectionHeaderIdentifier

private const val LOCAL_SITE_ID = 123
private val SECTION_HEADERS = listOf(
        SectionHeaderIdentifier(PUBLISHED),
        SectionHeaderIdentifier(DRAFTS),
        SectionHeaderIdentifier(SCHEDULED),
        SectionHeaderIdentifier(TRASHED)
)

class PostListItemDataSourceTest {
    private lateinit var dataSource: PostListItemDataSource
    private lateinit var postStore: PostStore

    @Before
    fun setup() {
        postStore = mock()
        dataSource = PostListItemDataSource(mock(), postStore, mock(), mock(), SEARCH)
    }

    private fun setupPostStoreMocks(postSummaries: List<PostSummary>, posts: List<PostModel>) {
        whenever(postStore.getPostSummaries(anyOrNull(), anyOrNull())).thenReturn(postSummaries)
        whenever(postStore.getPostsByLocalOrRemotePostIds(anyOrNull(), anyOrNull())).thenReturn(
                posts
        )
    }

    @Test
    fun `getItemIdentifiers when searching and results are in all categories`() {
        testSectionedItemIdentifiers(isListFullyFetched = true)
    }

    @Test
    fun `getItemIdentifiers list is not fully fetched`() {
        testSectionedItemIdentifiers(isListFullyFetched = false)
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in PUBLISHED`() {
        testSectionedItemIdentifiers(isListFullyFetched = true, excludeListTypes = listOf(PUBLISHED))
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in DRAFTS`() {
        testSectionedItemIdentifiers(isListFullyFetched = false, excludeListTypes = listOf(DRAFTS))
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in SCHEDULED`() {
        testSectionedItemIdentifiers(isListFullyFetched = true, excludeListTypes = listOf(SCHEDULED))
    }

    @Test
    fun `getItemIdentifiers when searching and results are in missing in TRASHED`() {
        testSectionedItemIdentifiers(isListFullyFetched = true, excludeListTypes = listOf(TRASHED))
    }

    @Test
    fun `getItemIdentifiers when searching and results are empty`() {
        testSectionedItemIdentifiers(isListFullyFetched = true, excludeListTypes = PostListType.values().toList())
    }

    /**
     * This helper function tests sectioning posts. When posts are sectioned, they are sorted by
     * their post list type that's decided through their status. Then, specific section headers are added at the
     * beginning of them. Finally, an [EndListIndicatorIdentifier] is added if all posts are fetched.
     *
     * In this helper, we want to test the following:
     * * All [SECTION_HEADERS] that are not excluded through [excludeListTypes] are present.
     * * [EndListIndicatorIdentifier] present if [isListFullyFetched] is true.
     * * Posts are put under the correct post list type.
     * * There are no [SECTION_HEADERS] that don't have any items under them.
     *
     * We achieve all this by going through the identifiers and updating a mutable list of expected [SECTION_HEADERS].
     * We do this in reverse order, because it's easier to keep track of the current section that way.
     *
     * For each identifier:
     * * If it's an end list indicator, we check if we were expecting one.
     * * If it's a post, we find its status by using the mocked data and then verify that it belongs to
     * the current section.
     * * If it's a section header, we check that it's the current section header and then pop the current section header
     * from the mutable list, so the next section becomes available.
     *
     * Finally, we verify that all expected sections are accounted for by checking that the mutable [SECTION_HEADERS]
     * list is empty.
     */
    private fun testSectionedItemIdentifiers(
        isListFullyFetched: Boolean,
        excludeListTypes: List<PostListType> = emptyList()
    ) {
        // Create postSummaries & posts to be used for remote and local posts respectively.
        val postSummaries = createPostSummaries(excludeListTypes)
        val posts = createLocalPosts(excludeListTypes)
        setupPostStoreMocks(postSummaries, posts)

        val identifiers = dataSource.getItemIdentifiers(
                mock(),
                remoteItemIds = emptyList(), // doesn't matter since we mocked the PostStore getters
                isListFullyFetched = isListFullyFetched
        )
        val getRemotePostStatus = { remotePostId: RemotePostId ->
            postSummaries.find { it.remoteId == remotePostId.id.value }!!.status
        }
        val getLocalPostStatus = { localPostId: LocalPostId ->
            PostStatus.fromPost(posts.find { it.id == localPostId.id.value }!!)
        }
        val mutableSectionHeaders = SECTION_HEADERS.filter { !excludeListTypes.contains(it.type) }.reversed()
                .toMutableList()
        identifiers.reversed().forEach { identifier ->
            val currentSection = mutableSectionHeaders.first()
            when (identifier) {
                is LocalPostId -> {
                    assertThat(PostListType.fromPostStatus(getLocalPostStatus(identifier)))
                            .isEqualTo(currentSection.type)
                }
                is RemotePostId -> {
                    assertThat(PostListType.fromPostStatus(getRemotePostStatus(identifier)))
                            .isEqualTo(currentSection.type)
                }
                EndListIndicatorIdentifier -> {
                    assertThat(isListFullyFetched).isTrue()
                }
                is SectionHeaderIdentifier -> {
                    // Check that the header is correct and then remove it so
                    assertThat(identifier).isEqualTo(currentSection)
                    mutableSectionHeaders.remove(currentSection)
                }
            }
        }
        assertThat(mutableSectionHeaders).isEmpty()
    }

    private fun createLocalPosts(excludeListTypes: List<PostListType> = emptyList()): List<PostModel> =
            createPostStatuses(excludeListTypes).mapIndexed { i, status ->
                PostModel().also {
                    it.setId(i)
                    it.setStatus(status.toString())
                }
            }

    private fun createPostSummaries(excludeListTypes: List<PostListType> = emptyList()): List<PostSummary> =
            createPostStatuses(excludeListTypes).mapIndexed { i, status ->
                PostSummary(remoteId = i.toLong() + 1, status = status, localSiteId = LOCAL_SITE_ID)
            }

    private fun createPostStatuses(excludeListTypes: List<PostListType> = emptyList()): List<PostStatus> =
            listOf(
                    PostStatus.PUBLISHED,
                    PostStatus.DRAFT,
                    PostStatus.PENDING,
                    PostStatus.SCHEDULED,
                    PostStatus.PRIVATE,
                    PostStatus.TRASHED
            ).filter { !excludeListTypes.contains(PostListType.fromPostStatus(it)) }
}
