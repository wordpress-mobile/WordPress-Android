package org.wordpress.android.fluxc.page

import com.nhaarman.mockitokotlin2.mock
import com.yarolegovich.wellsql.WellSql
import kotlinx.coroutines.Dispatchers
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
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class PageStoreLocalDraftTest {
    private val pageStore = PageStore(postStore = mock(), dispatcher = mock(), coroutineContext = Dispatchers.Default)

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
            createLocalDraft(localSiteId = site.id, baseTitle = baseTitle).apply {
                setIsPage(true)
            }
        }

        val unexpectedPosts = listOf(
                // local draft post
                createLocalDraft(localSiteId = site.id).apply { setIsPage(false) },
                // other site page
                createLocalDraft(localSiteId = 4_000),
                // uploaded page
                PostModel().apply {
                    title = "Title"
                    localSiteId = site.id
                    setIsLocalDraft(false)
                }
        )

        expectedPages.plus(unexpectedPosts).forEach { PostSqlUtils.insertPostForResult(it) }

        // Act
        val localDraftPages = pageStore.getLocalDraftPages(site)

        // Assert
        assertThat(localDraftPages).hasSize(3)
        assertThat(localDraftPages).allMatch { it.title.startsWith(baseTitle) }
        assertThat(localDraftPages.map { it.id }).isEqualTo(expectedPages.map { it.id })
    }

    private fun createLocalDraft(localSiteId: Int, baseTitle: String = "Title") = PostModel().apply {
        this.localSiteId = localSiteId
        title = "$baseTitle:${UUID.randomUUID()}"
        setIsLocalDraft(true)
        status = PostStatus.DRAFT.toString()
    }
}
