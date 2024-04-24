package org.wordpress.android.viewmodel.pages

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.UploadStore
import kotlin.test.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.wordpress.android.ui.posts.PostConflictDetector

@Suppress("UNCHECKED_CAST")
@ExperimentalCoroutinesApi
class PageConflictDetectorTest : BaseUnitTest() {
    private val uploadStore: UploadStore = mock()

    // Class under test
    private lateinit var pageConflictDetector: PageConflictDetector

    @Before
    fun setUp() {
        pageConflictDetector = PageConflictDetector(PostConflictDetector(uploadStore))
    }

    @Test
    fun `given upload store with unhandled conflict, when does page have unhandled conflict is invoked, then true`() {
        val post = PostModel()
        whenever(uploadStore.getUploadErrorForPost(post)).thenReturn(
            UploadStore.UploadError(PostStore.PostError(PostStore.PostErrorType.OLD_REVISION))
        )

        val result = pageConflictDetector.hasUnhandledConflict(post)

        assertTrue(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `given upload store with no unhandled conflict, when page have unhandled conflict is invoked, then false`() {
        val post = PostModel()
        whenever(uploadStore.getUploadErrorForPost(post)).thenReturn(null)

        val result = pageConflictDetector.hasUnhandledConflict(post)

        assertFalse(result)
    }

    @Test
    fun `given page with unhandled auto save, when has unhandled auto save is invoked, then true`() {
        val post = PostModel().apply {
            setIsLocallyChanged(false)
            setAutoSaveRevisionId(1)
            setAutoSaveExcerpt("Some auto save excerpt")
        }

        val result = pageConflictDetector.hasUnhandledAutoSave(post)

        assertTrue(result)
    }

    @Test
    fun `given page with no unhandled auto save, when has unhandled auto save is invoked, then false`() {
        val post = PostModel().apply {
            setIsLocallyChanged(true)
        }

        val result = pageConflictDetector.hasUnhandledAutoSave(post)

        assertFalse(result)
    }
}

