package org.wordpress.android.ui.rs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class RsLocalDraftsSectionTest : BaseUnitTest(StandardTestDispatcher()) {
    @Mock lateinit var source: RsLocalDrafts
    @Mock lateinit var resourceProvider: ResourceProvider

    private val site = SiteModel().apply { id = SITE_ID }
    private val localChanges = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var query = ""

    @Before
    fun setUp() {
        whenever(resourceProvider.getString(any())).thenReturn("label")
        stubLoad(emptyList())
    }

    @Test
    fun `starting lists the drafts and queues the uploads that are due`() = test {
        stubLoad(listOf(draft()))
        val section = section()

        section.start(localChanges)
        runCurrent()

        assertThat(section.drafts.value).containsExactly(draft())
        verify(source).queueDueUploads(site)
    }

    @Test
    fun `a local change reloads the drafts`() = test {
        val section = section()
        section.start(localChanges)
        runCurrent()
        stubLoad(listOf(draft()))

        localChanges.emit(Unit)
        runCurrent()

        assertThat(section.drafts.value).containsExactly(draft())
    }

    @Test
    fun `each load reads the query it was asked with`() = test {
        val section = section()

        query = "banana"
        section.reload()
        runCurrent()

        verifyBlocking(source) { load(eq(site), eq(false), eq("banana"), any(), any()) }
    }

    @Test
    fun `pages are loaded as pages`() = test {
        section(isPage = true).reload()
        runCurrent()

        verifyBlocking(source) { load(eq(site), eq(true), any(), any(), any()) }
    }

    @Test
    fun `a user refresh queues uploads and reloads`() = test {
        val section = section()

        section.onUserRefresh()
        runCurrent()

        verify(source).queueDueUploads(site)
        verifyBlocking(source) { load(any(), any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `opening a listed draft hands it over without reloading`() = test {
        val post = PostModel()
        whenever(source.open(site, LOCAL_ID)).thenReturn(post)
        val section = section()

        assertThat(section.open(LOCAL_ID)).isSameAs(post)
        runCurrent()

        verifyBlocking(source, never()) { load(any(), any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `opening a draft that has gone reloads instead`() = test {
        val section = section()

        assertThat(section.open(LOCAL_ID)).isNull()
        runCurrent()

        verifyBlocking(source) { load(any(), any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `retry restarts the upload and reloads`() = test {
        val section = section()

        section.retry(LOCAL_ID)
        runCurrent()

        verify(source).retry(site, LOCAL_ID)
        verifyBlocking(source) { load(any(), any(), any(), any(), anyOrNull()) }
    }

    @Test
    fun `delete removes the draft`() = test {
        section().delete(LOCAL_ID)

        verify(source).delete(site, LOCAL_ID)
    }

    /** On the background scope: the section's change collector never completes, and runTest would wait on it. */
    private fun TestScope.section(isPage: Boolean = false) =
        RsLocalDraftsSection(backgroundScope, source, resourceProvider, site, isPage) { query }

    private fun stubLoad(drafts: List<RsLocalDraftUiModel>) {
        source.stub { on { load(any(), any(), any(), any(), anyOrNull()) } doReturn drafts }
    }

    private fun draft() = RsLocalDraftUiModel(
        localId = LOCAL_ID,
        title = "Unsent",
        excerpt = "",
        dateLabel = "label",
        statusLabels = emptyList(),
        isUploadFailed = true
    )

    private companion object {
        const val SITE_ID = 3
        const val LOCAL_ID = 11
    }
}
