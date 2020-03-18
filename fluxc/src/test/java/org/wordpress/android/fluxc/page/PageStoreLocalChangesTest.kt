package org.wordpress.android.fluxc.page

import com.nhaarman.mockitokotlin2.mock
import com.yarolegovich.wellsql.WellSql
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PageStore
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine
import java.util.UUID
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class PageStoreLocalChangesTest {
    private val postSqlUtils = PostSqlUtils()
    private val pageStore = PageStore(
            postStore = mock(),
            dispatcher = mock(),
            coroutineEngine = initCoroutineEngine(),
            postSqlUtils = postSqlUtils
    )

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val modelsToTest = listOf(PostModel::class.java)
        val config = SingleStoreWellSqlConfigForTests(appContext, modelsToTest, "")
        WellSql.init(config)
        config.reset()
    }

    @After
    fun tearDown() {
        WellSql.closeDb()
    }

    @Test
    fun `getLocalDraftPages returns local draft pages only`() = test {
        // Arrange
        val site = SiteModel().apply { id = 3_000 }

        val baseTitle = "Voluptatem harum repellendus"
        val expectedPages = List(3) {
            createLocalDraft(localSiteId = site.id, baseTitle = baseTitle, isPage = true)
        }

        val unexpectedPosts = listOf(
                // local draft post
                createLocalDraft(localSiteId = site.id, isPage = false),
                // other site page
                createLocalDraft(localSiteId = 4_000, isPage = true),
                // uploaded page
                PostModel().apply {
                    setTitle("Title")
                    setLocalSiteId(site.id)
                    setIsLocalDraft(false)
                }
        )

        expectedPages.plus(unexpectedPosts).forEach { postSqlUtils.insertPostForResult(it) }

        // Act
        val localDraftPages = pageStore.getLocalDraftPages(site)

        // Assert
        assertThat(localDraftPages).hasSize(3)
        assertThat(localDraftPages).allMatch { it.title.startsWith(baseTitle) }
        assertThat(localDraftPages.map { it.id }).isEqualTo(expectedPages.map { it.id })
    }

    @Test
    fun `getPagesWithLocalChanges returns local draft and locally changed pages only`() = test {
        // Arrange
        val site = SiteModel().apply { id = 3_000 }

        val baseTitle = "Voluptatem harum repellendus"
        val expectedPages = mutableListOf<PostModel>().apply {
            addAll(List(3) {
                createLocalDraft(localSiteId = site.id, baseTitle = baseTitle, isPage = true)
            })

            addAll(List(5) {
                createUploadedPost(localSiteId = site.id, baseTitle = baseTitle, isPage = true).apply {
                    setIsLocallyChanged(true)
                }
            })

            add(createUploadedPost(localSiteId = site.id, baseTitle = baseTitle, isPage = true).apply {
                setStatus(PostStatus.PUBLISHED.toString())
                setIsLocallyChanged(true)
            })
        }.toList()

        val unexpectedPosts = listOf(
                // local draft post
                createLocalDraft(localSiteId = site.id, isPage = false),
                // other site page
                createLocalDraft(localSiteId = 4_000, isPage = true),
                // uploaded post
                createUploadedPost(localSiteId = site.id, isPage = false),
                // uploaded post with changes
                createUploadedPost(localSiteId = site.id, isPage = false).apply { setIsLocallyChanged(true) },
                // uploaded page with no changes
                createUploadedPost(localSiteId = site.id, isPage = true),
                // published page with no changes
                createUploadedPost(localSiteId = site.id, isPage = true).apply {
                    setStatus(PostStatus.PUBLISHED.toString())
                }
        )

        expectedPages.plus(unexpectedPosts).forEach { postSqlUtils.insertPostForResult(it) }

        // Act
        val locallyChangedPages = pageStore.getPagesWithLocalChanges(site)

        // Assert
        assertThat(locallyChangedPages).hasSize(expectedPages.size)
        assertThat(locallyChangedPages).allMatch { it.title.startsWith(baseTitle) }
        assertThat(locallyChangedPages.map { it.id }).isEqualTo(expectedPages.map { it.id })
    }

    private fun createLocalDraft(localSiteId: Int, baseTitle: String = "Title", isPage: Boolean) = PostModel().apply {
        setLocalSiteId(localSiteId)
        setTitle("$baseTitle:${UUID.randomUUID()}")
        setIsPage(isPage)
        setIsLocalDraft(true)
        setStatus(PostStatus.DRAFT.toString())
    }

    private fun createUploadedPost(localSiteId: Int, baseTitle: String = "Title", isPage: Boolean) = PostModel().apply {
        setLocalSiteId(localSiteId)
        setRemotePostId(Random.nextLong())
        setTitle("$baseTitle:${UUID.randomUUID()}")
        setIsPage(isPage)
    }
}
