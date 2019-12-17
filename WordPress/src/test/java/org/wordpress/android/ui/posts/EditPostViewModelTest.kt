package org.wordpress.android.ui.posts

import android.content.Context
import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PRIVATE
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.fluxc.model.post.PostStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus.TRASHED
import org.wordpress.android.fluxc.model.post.PostStatus.UNKNOWN
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateFromEditor.PostFields
import org.wordpress.android.ui.posts.EditPostViewModel.UpdateResult
import org.wordpress.android.ui.uploads.UploadServiceFacade
import org.wordpress.android.util.LocaleManagerWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import java.util.Calendar
import java.util.TimeZone

class EditPostViewModelTest : BaseUnitTest() {
    @Mock lateinit var localeManagerWrapper: LocaleManagerWrapper
    @Mock lateinit var siteStore: SiteStore
    @Mock lateinit var postUtils: PostUtilsWrapper
    @Mock lateinit var uploadService: UploadServiceFacade
    @Mock lateinit var postRepository: EditPostRepository
    @Mock lateinit var savePostToDbUseCase: SavePostToDbUseCase
    @Mock lateinit var networkUtils: NetworkUtilsWrapper
    @Mock lateinit var context: Context

    private lateinit var transactionCaptor: KArgumentCaptor<(PostModel) -> Boolean>
    private lateinit var updateResultCaptor: KArgumentCaptor<(PostModel) -> UpdateResult>
    private lateinit var actionCaptor: KArgumentCaptor<Action<PostModel>>

    private lateinit var viewModel: EditPostViewModel
    private val title = "title"
    private val updatedTitle = "updatedTitle"
    private val content = "content"
    private val updatedContent = "updatedContent"
    private val currentTime = "2019-11-10T11:10:00+0100"
    private val postStatus = "DRAFT"
    private val postModel = PostModel()
    private val site = SiteModel()
    private val localSiteId = 1
    private val immutablePost: PostImmutableModel = postModel
    private val postId = 2

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = EditPostViewModel(
                TEST_DISPATCHER,
                siteStore,
                postUtils,
                uploadService,
                savePostToDbUseCase,
                networkUtils
        )
        transactionCaptor = argumentCaptor()
        updateResultCaptor = argumentCaptor()
        actionCaptor = argumentCaptor()
        setupCurrentTime()
        postModel.setTitle(title)
        postModel.setContent(content)
        postModel.setStatus(postStatus)
        whenever(postRepository.getEditablePost()).thenReturn(postModel)
        whenever(postRepository.content).thenReturn(content)
    }

    @Test
    fun `delays save call`() {
        var event: Event<Unit>? = null
        viewModel.onSavePostTriggered.observeForever {
            event = it
        }
        assertThat(event).isNull()

        viewModel.savePostWithDelay()

        assertThat(event).isNotNull()
    }

    @Test
    fun `saves post to DB`() {
        whenever(postRepository.postHasChangesFromDb()).thenReturn(true)

        viewModel.savePostToDb(context, postRepository, site)

        assertThat(actionCaptor.firstValue.type).isEqualTo(PostAction.UPDATE_POST)
        assertThat(actionCaptor.firstValue.payload).isEqualTo(postModel)
        verify(postRepository).saveDbSnapshot()
    }

    @Test
    fun `does not save the post with no change`() {
        whenever(postRepository.postHasChangesFromDb()).thenReturn(false)

        viewModel.savePostToDb(context, postRepository, site)

        verify(postRepository, never()).saveDbSnapshot()
    }

    @Test
    fun `does not update post object with no change`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObjectWithUI(postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(false))
    }

    @Test
    fun `returns update error when post is missing`() {
        whenever(postRepository.hasPost()).thenReturn(false)

        val result = viewModel.updatePostObjectWithUI(postRepository) {
            PostFields(
                    title,
                    content
            )
        }

        assertThat(result).isEqualTo(UpdateResult.Error)
    }

    @Test
    fun `returns update error when get content function returns null`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObjectWithUI(postRepository) {
            UpdateFromEditor.Failed(
                    RuntimeException("Not found")
            )
        }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Error)
    }

    @Test
    fun `updates post title and date locally changed when title has changed`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObjectWithUI(postRepository) {
            PostFields(
                    updatedTitle,
                    content
            )
        }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        assertThat(postModel.title).isEqualTo(updatedTitle)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
    }

    @Test
    fun `updates post content and date locally changed when content has changed`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObjectWithUI(postRepository) {
            PostFields(
                    title,
                    updatedContent
            )
        }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(true))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
        assertThat(postModel.content).isEqualTo(updatedContent)
        verify(postRepository).updatePublishDateIfShouldBePublishedImmediately(postModel)
    }

    @Test
    fun `updates post date when status has changed`() {
        whenever(postRepository.hasPost()).thenReturn(true)

        viewModel.updatePostObjectWithUI(postRepository) { PostFields(title, content) }

        verify(postRepository).updateInTransaction(updateResultCaptor.capture())

        val result = updateResultCaptor.firstValue.invoke(postModel)

        assertThat(result).isEqualTo(UpdateResult.Success(false))
        assertThat(postModel.dateLocallyChanged).isEqualTo(currentTime)
    }

    @Test
    fun `savePostOnline changes post status to PENDING when user can't publish`() {
        val isFirstTimePublish = true
        listOf(UNKNOWN, PUBLISHED, SCHEDULED, PRIVATE).forEach { status ->
            reset(postRepository)
            setupPostRepository(status, userCanPublish = false)

            viewModel.savePostOnline(
                    isFirstTimePublish,
                    context,
                    postRepository,
                    site
            )

            verify(postRepository).setStatus(PENDING)
        }
    }

    @Test
    fun `savePostOnline doesn't change status when user can't publish and post is DRAFT, PENDING or TRASHED`() {
        val isFirstTimePublish = true
        listOf(PostStatus.DRAFT, PENDING, TRASHED).forEach { status ->
            reset(postRepository)
            setupPostRepository(status, userCanPublish = false)

            viewModel.savePostOnline(
                    isFirstTimePublish,
                    context,
                    postRepository,
                    site
            )

            verify(postRepository, never()).setStatus(any())
        }
    }

    @Test
    fun `savePostOnline saves post to DB when there are changes`() {
        val isFirstTimePublish = true

        setupPostRepository(PUBLISHED)
        whenever(postRepository.postHasChangesFromDb()).thenReturn(true)

        viewModel.savePostOnline(
                isFirstTimePublish,
                context,
                postRepository,
                site
        )
        verify(postRepository).saveDbSnapshot()

        assertThat(actionCaptor.firstValue.type).isEqualTo(PostAction.UPDATE_POST)
        assertThat(actionCaptor.firstValue.payload).isEqualTo(postModel)
    }

    @Test
    fun `savePostOnline does not save post to DB when there are no changes`() {
        val isFirstTimePublish = true

        setupPostRepository(PUBLISHED)
        whenever(postRepository.postHasChangesFromDb()).thenReturn(false)

        viewModel.savePostOnline(
                isFirstTimePublish,
                context,
                postRepository,
                site
        )
    }

    @Test
    fun `savePostOnline uploads post online, tracks result and finishes`() {
        val isFirstTimePublish = true

        setupPostRepository(PUBLISHED)
        whenever(postRepository.postHasChangesFromDb()).thenReturn(false)
        var finished = false
        viewModel.onFinish.observeForever {
            it.applyIfNotHandled { finished = true }
        }

        viewModel.savePostOnline(
                isFirstTimePublish,
                context,
                postRepository,
                site
        )

        verify(postUtils).trackSavePostAnalytics(immutablePost, site)
        verify(uploadService).uploadPost(context, postId, isFirstTimePublish)
        assertThat(finished).isTrue()
    }

    private fun setupPostRepository(
        postStatus: PostStatus,
        userCanPublish: Boolean = true
    ) {
        whenever(postRepository.status).thenReturn(postStatus)
        whenever(postRepository.getPost()).thenReturn(immutablePost)
        whenever(postRepository.hasPost()).thenReturn(true)
        whenever(postRepository.localSiteId).thenReturn(localSiteId)
        whenever(postRepository.id).thenReturn(postId)
        whenever(siteStore.getSiteByLocalId(localSiteId)).thenReturn(site)
        whenever(postRepository.updateInTransaction<Any>(any())).then {
            (it.arguments[0] as ((PostModel) -> Any)).invoke(postModel)
        }
    }

    private fun setupCurrentTime() {
        val now = Calendar.getInstance()
        now.set(2019, 10, 10, 10, 10, 0)
        now.timeZone = TimeZone.getTimeZone("UTC")
        whenever(localeManagerWrapper.getCurrentCalendar()).thenReturn(now)
    }
}
