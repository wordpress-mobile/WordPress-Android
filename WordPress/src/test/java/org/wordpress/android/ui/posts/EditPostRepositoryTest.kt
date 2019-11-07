package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostLocation
import org.wordpress.android.fluxc.model.post.PostStatus.DRAFT
import org.wordpress.android.fluxc.model.post.PostStatus.PENDING
import org.wordpress.android.fluxc.model.post.PostStatus.PUBLISHED
import org.wordpress.android.util.LocaleManagerWrapper
import java.util.Calendar
import java.util.TimeZone

@RunWith(MockitoJUnitRunner::class)
class EditPostRepositoryTest {
    @Mock lateinit var localeManager: LocaleManagerWrapper
    @Mock lateinit var postUtils: PostUtilsWrapper
    private lateinit var editPostRepository: EditPostRepository
    @Before
    fun setUp() {
        editPostRepository = EditPostRepository(localeManager, postUtils)
    }

    @Test
    fun `reads post correctly`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()).isEqualTo(post)
        assertThat(editPostRepository.hasPost()).isTrue()
    }

    @Test
    fun `does not have post before initialization`() {
        assertThat(editPostRepository.hasPost()).isFalse()
    }

    @Test
    fun `is not publishable before initialization`() {
        assertThat(editPostRepository.isPostPublishable()).isFalse()
    }

    @Test
    fun `reads post for undo correctly`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        editPostRepository.saveForUndo()

        assertThat(editPostRepository.getPostForUndo()).isEqualTo(post)
    }

    @Test
    fun `reads id correctly`() {
        val post = PostModel()
        val id = 10
        post.id = id

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.id).isEqualTo(id)
        assertThat(editPostRepository.id).isEqualTo(id)
    }

    @Test
    fun `reads localSiteId correctly`() {
        val post = PostModel()
        val localSiteId = 10
        post.localSiteId = localSiteId

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.localSiteId).isEqualTo(localSiteId)
        assertThat(editPostRepository.localSiteId).isEqualTo(localSiteId)
    }

    @Test
    fun `reads remotePostId correctly`() {
        val post = PostModel()
        val remotePostId = 10L
        post.remotePostId = remotePostId

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.remotePostId).isEqualTo(remotePostId)
        assertThat(editPostRepository.remotePostId).isEqualTo(remotePostId)
    }

    @Test
    fun `reads title correctly`() {
        val post = PostModel()
        val title = "title"
        post.title = title

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.title).isEqualTo(title)
        assertThat(editPostRepository.title).isEqualTo(title)
    }

    @Test
    fun `reads autoSaveTitle correctly`() {
        val post = PostModel()
        val autoSaveTitle = "autoSaveTitle"
        post.autoSaveTitle = autoSaveTitle

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.autoSaveTitle).isEqualTo(autoSaveTitle)
        assertThat(editPostRepository.autoSaveTitle).isEqualTo(autoSaveTitle)
    }

    @Test
    fun `reads content correctly`() {
        val post = PostModel()
        val content = "content"
        post.content = content

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.content).isEqualTo(content)
        assertThat(editPostRepository.content).isEqualTo(content)
    }

    @Test
    fun `reads autoSaveContent correctly`() {
        val post = PostModel()
        val autoSaveContent = "autoSaveContent"
        post.autoSaveContent = autoSaveContent

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.autoSaveContent).isEqualTo(autoSaveContent)
        assertThat(editPostRepository.autoSaveContent).isEqualTo(autoSaveContent)
    }

    @Test
    fun `reads excerpt correctly`() {
        val post = PostModel()
        val excerpt = "excerpt"
        post.excerpt = excerpt

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.excerpt).isEqualTo(excerpt)
        assertThat(editPostRepository.excerpt).isEqualTo(excerpt)
    }

    @Test
    fun `reads autoSaveExcerpt correctly`() {
        val post = PostModel()
        val autoSaveExcerpt = "autoSaveExcerpt"
        post.autoSaveExcerpt = autoSaveExcerpt

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
        assertThat(editPostRepository.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
    }

    @Test
    fun `reads password correctly`() {
        val post = PostModel()
        val password = "password"
        post.password = password

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.password).isEqualTo(password)
        assertThat(editPostRepository.password).isEqualTo(password)
    }

    @Test
    fun `reads status correctly`() {
        val post = PostModel()
        val status = DRAFT
        post.status = status.toString()

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.status).isEqualTo(status.toString())
        assertThat(editPostRepository.status).isEqualTo(status)
    }

    @Test
    fun `reads isPage correctly`() {
        val post = PostModel()
        val isPage = true
        post.setIsPage(isPage)

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.isPage).isEqualTo(isPage)
        assertThat(editPostRepository.isPage).isEqualTo(isPage)
    }

    @Test
    fun `reads isLocalDraft correctly`() {
        val post = PostModel()
        val isLocalDraft = true
        post.setIsLocalDraft(isLocalDraft)

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.isLocalDraft).isEqualTo(isLocalDraft)
        assertThat(editPostRepository.isLocalDraft).isEqualTo(isLocalDraft)
    }

    @Test
    fun `reads isLocallyChanged correctly`() {
        val post = PostModel()
        val isLocallyChanged = true
        post.setIsLocallyChanged(isLocallyChanged)

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.isLocallyChanged).isEqualTo(isLocallyChanged)
        assertThat(editPostRepository.isLocallyChanged).isEqualTo(isLocallyChanged)
    }

    @Test
    fun `reads featuredImageId correctly`() {
        val post = PostModel()
        val featuredImageId = 10L
        post.featuredImageId = featuredImageId

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.featuredImageId).isEqualTo(featuredImageId)
        assertThat(editPostRepository.featuredImageId).isEqualTo(featuredImageId)
    }

    @Test
    fun `reads dateCreated correctly`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"
        post.dateCreated = dateCreated

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `reads changesConfirmedContentHashcode correctly`() {
        val post = PostModel()
        val changesConfirmedContentHashcode = 10
        post.changesConfirmedContentHashcode = changesConfirmedContentHashcode

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.changesConfirmedContentHashcode).isEqualTo(
                changesConfirmedContentHashcode
        )
        assertThat(editPostRepository.changesConfirmedContentHashcode).isEqualTo(
                changesConfirmedContentHashcode
        )
    }

    @Test
    fun `reads postFormat correctly`() {
        val post = PostModel()
        val postFormat = "format"
        post.postFormat = postFormat

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.postFormat).isEqualTo(postFormat)
        assertThat(editPostRepository.postFormat).isEqualTo(postFormat)
    }

    @Test
    fun `reads slug correctly`() {
        val post = PostModel()
        val slug = "slug"
        post.slug = slug

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.slug).isEqualTo(slug)
        assertThat(editPostRepository.slug).isEqualTo(slug)
    }

    @Test
    fun `reads link correctly`() {
        val post = PostModel()
        val link = "link"
        post.link = link

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.link).isEqualTo(link)
        assertThat(editPostRepository.link).isEqualTo(link)
    }

    @Test
    fun `reads location correctly`() {
        val post = PostModel()
        val location = PostLocation(20.0, 30.0)
        post.location = location

        editPostRepository.setPost(post)

        assertThat(editPostRepository.getPost()!!.location).isEqualTo(location)
        assertThat(editPostRepository.location).isEqualTo(location)
        assertThat(editPostRepository.hasLocation()).isTrue()
    }

    @Test
    fun `updates publish date when should publish immediately`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 1572000000000
        calendar.timeZone = TimeZone.getTimeZone("UTC")
        val now = "2019-10-25T10:40:00+0000"

        whenever(localeManager.getCurrentCalendar()).thenReturn(calendar)

        post.status = DRAFT.toString()
        post.dateCreated = dateCreated
        editPostRepository.setPost(post)
        whenever(postUtils.shouldPublishImmediately(DRAFT, dateCreated)).thenReturn(true)

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately(post)

        assertThat(editPostRepository.getPost()!!.dateCreated).isEqualTo(now)
        assertThat(editPostRepository.dateCreated).isEqualTo(now)
    }

    @Test
    fun `does not update publish date when should not publish immediately`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 1572000000000
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        post.status = PUBLISHED.toString()
        post.dateCreated = dateCreated
        editPostRepository.setPost(post)
        whenever(postUtils.shouldPublishImmediately(PUBLISHED, dateCreated)).thenReturn(false)

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately(post)

        assertThat(editPostRepository.getPost()!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `is not publishable when isPublishable(post) is false`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        whenever(postUtils.isPublishable(post)).thenReturn(false)

        assertThat(editPostRepository.isPostPublishable()).isFalse()
    }

    @Test
    fun `is publishable when isPublishable(post) is true`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        whenever(postUtils.isPublishable(post)).thenReturn(true)

        assertThat(editPostRepository.isPostPublishable()).isTrue()
    }

    @Test
    fun `test undo`() {
        val firstPost = PostModel()
        val firstPostId = 1
        firstPost.id = firstPostId
        val secondPost = PostModel()
        val secondPostId = 2
        secondPost.id = secondPostId

        editPostRepository.setPost(firstPost)

        assertThat(editPostRepository.getPost()).isEqualTo(firstPost)

        editPostRepository.saveForUndo()

        editPostRepository.setPost(secondPost)

        assertThat(editPostRepository.getPost()).isEqualTo(secondPost)

        editPostRepository.undo()

        assertThat(editPostRepository.getPost()).isEqualTo(firstPost)
    }

    @Test
    fun `saves snapshot`() {
        val firstPost = PostModel()
        val firstPostId = 1
        firstPost.id = firstPostId

        editPostRepository.setPost(firstPost)

        assertThat(editPostRepository.getPost()).isEqualTo(firstPost)

        editPostRepository.saveSnapshot()

        assertThat(editPostRepository.hasSnapshot()).isTrue()
    }

    @Test
    fun `snapshot is different when original post is changes`() {
        val firstPost = PostModel()
        val firstPostId = 1
        firstPost.id = firstPostId
        val secondPost = PostModel()
        val secondPostId = 2
        secondPost.id = secondPostId

        editPostRepository.setPost(firstPost)

        assertThat(editPostRepository.getPost()).isEqualTo(firstPost)

        editPostRepository.saveSnapshot()

        assertThat(editPostRepository.isSnapshotDifferent()).isFalse()

        editPostRepository.setPost(secondPost)

        assertThat(editPostRepository.isSnapshotDifferent()).isTrue()
    }

    @Test
    fun `updates status from snapshot`() {
        val firstPost = PostModel()
        val firstPostStatus = PUBLISHED
        firstPost.status = firstPostStatus.toString()
        val secondPost = PostModel()
        val secondPostStatus = PENDING
        secondPost.status = secondPostStatus.toString()

        editPostRepository.setPost(firstPost)

        editPostRepository.saveSnapshot()

        editPostRepository.setPost(secondPost)

        assertThat(editPostRepository.status).isEqualTo(PENDING)

        editPostRepository.updateStatusFromSnapshot(secondPost)

        assertThat(editPostRepository.status).isEqualTo(PUBLISHED)
    }
}
