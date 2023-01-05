package org.wordpress.android.ui.posts

import androidx.lifecycle.Observer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.network.rest.wpcom.post.PostRestClient
import org.wordpress.android.fluxc.network.xmlrpc.post.PostXMLRPCClient
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.ui.posts.PostListAction.EditPost

@ExperimentalCoroutinesApi
class PostListMainViewModelCopyPostTest : BaseUnitTest() {
    @Mock lateinit var site: SiteModel
    @Mock lateinit var dispatcher: Dispatcher
    @Mock lateinit var postSqlUtils: PostSqlUtils
    @Mock lateinit var onPostListActionObserver: Observer<PostListAction>

    private lateinit var viewModel: PostListMainViewModel
    private lateinit var postStore: PostStore

    private val copyPostId = 2
    private val mockedPost = PostModel().apply {
        setId(1)
        setTitle("mockedTitle")
        setContent("mockedContent")
        setCategoryIdList(listOf()) // Using an empty list to avoid invoking android.text.TextUtils.join
        setPostFormat("mockedPostFormat")
    }

    @Before
    fun setUp() {
        postStore = PostStore(
                dispatcher,
                Mockito.mock(PostRestClient::class.java),
                Mockito.mock(PostXMLRPCClient::class.java),
                postSqlUtils
        )
        viewModel = PostListMainViewModel(
                dispatcher = dispatcher,
                postStore = postStore,
                accountStore = mock(),
                uploadStore = mock(),
                mediaStore = mock(),
                networkUtilsWrapper = mock(),
                prefs = mock(),
                previewStateHelper = mock(),
                analyticsTracker = mock(),
                mainDispatcher = testDispatcher(),
                bgDispatcher = testDispatcher(),
                postListEventListenerFactory = mock(),
                uploadStarter = mock(),
                uploadActionUseCase = mock(),
                savePostToDbUseCase = mock(),
                jetpackFeatureRemovalPhaseHelper = mock()
        )
        viewModel.postListAction.observeForever(onPostListActionObserver)

        whenever(postSqlUtils.insertPostForResult(any())).thenAnswer { invocation ->
            (invocation.arguments[0] as PostModel).apply { setId(copyPostId) }
        }
    }

    @Test
    fun `when the user copies a post the editor opens`() {
        viewModel.copyPost(site, mockedPost)
        val captor = ArgumentCaptor.forClass(PostListAction::class.java)
        verify(onPostListActionObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value is EditPost))
    }

    @Test
    fun `when the user copies a post a new post is created`() {
        viewModel.copyPost(site, mockedPost)
        val captor = ArgumentCaptor.forClass(PostListAction::class.java)
        verify(onPostListActionObserver).onChanged(captor.capture())
        val newPost = requireNotNull(captor.value as? EditPost).post
        assertThat(newPost.id).isNotEqualTo(mockedPost.id)
        assertThat(newPost.id).isEqualTo(copyPostId)
    }

    @Test
    fun `when the user copies a post the title, content, categories and post format is copied in the new post`() {
        viewModel.copyPost(site, mockedPost)
        val captor = ArgumentCaptor.forClass(PostListAction::class.java)
        verify(onPostListActionObserver).onChanged(captor.capture())
        val newPost = requireNotNull(captor.value as? EditPost).post
        assertThat(newPost.title).isEqualTo(mockedPost.title)
        assertThat(newPost.content).isEqualTo(mockedPost.content)
        assertThat(newPost.categoryIdList).isEqualTo(mockedPost.categoryIdList)
        assertThat(newPost.postFormat).isEqualTo(mockedPost.postFormat)
    }

    @Test
    fun `when the user copies a published post a draft copy of the post is opened for edit`() {
        viewModel.copyPost(site, mockedPost.apply { setStatus(PostStatus.PUBLISHED.toString()) })
        val captor = ArgumentCaptor.forClass(PostListAction::class.java)
        verify(onPostListActionObserver).onChanged(captor.capture())
        assertThat(requireNotNull(captor.value is EditPost))
        assertThat((captor.value as EditPost).post.status).isEqualTo(PostStatus.DRAFT.toString())
    }
}
