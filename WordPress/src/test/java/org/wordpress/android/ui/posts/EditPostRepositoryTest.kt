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
    fun `sets and reads post correctly`() {
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
        assertThat(editPostRepository.isPublishable()).isFalse()
    }

    @Test
    fun `sets and reads post for undo correctly`() {
        val post = PostModel()

        editPostRepository.postForUndo = post

        assertThat(editPostRepository.postForUndo).isEqualTo(post)
    }

    @Test
    fun `sets and reads post snapshot correctly`() {
        val post = PostModel()

        editPostRepository.postSnapshotWhenEditorOpened = post

        assertThat(editPostRepository.postSnapshotWhenEditorOpened).isEqualTo(post)
    }

    @Test
    fun `sets and reads id correctly`() {
        val post = PostModel()
        val id = 10

        editPostRepository.setPost(post)
        editPostRepository.id = id

        assertThat(editPostRepository.getPost()!!.id).isEqualTo(id)
        assertThat(editPostRepository.id).isEqualTo(id)
    }

    @Test
    fun `sets and reads localSiteId correctly`() {
        val post = PostModel()
        val localSiteId = 10

        editPostRepository.setPost(post)
        editPostRepository.localSiteId = localSiteId

        assertThat(editPostRepository.getPost()!!.localSiteId).isEqualTo(localSiteId)
        assertThat(editPostRepository.localSiteId).isEqualTo(localSiteId)
    }

    @Test
    fun `sets and reads remotePostId correctly`() {
        val post = PostModel()
        val remotePostId = 10L

        editPostRepository.setPost(post)
        editPostRepository.remotePostId = remotePostId

        assertThat(editPostRepository.getPost()!!.remotePostId).isEqualTo(remotePostId)
        assertThat(editPostRepository.remotePostId).isEqualTo(remotePostId)
    }

    @Test
    fun `sets and reads title correctly`() {
        val post = PostModel()
        val title = "title"

        editPostRepository.setPost(post)
        editPostRepository.title = title

        assertThat(editPostRepository.getPost()!!.title).isEqualTo(title)
        assertThat(editPostRepository.title).isEqualTo(title)
    }

    @Test
    fun `sets and reads autoSaveTitle correctly`() {
        val post = PostModel()
        val autoSaveTitle = "autoSaveTitle"

        editPostRepository.setPost(post)
        editPostRepository.autoSaveTitle = autoSaveTitle

        assertThat(editPostRepository.getPost()!!.autoSaveTitle).isEqualTo(autoSaveTitle)
        assertThat(editPostRepository.autoSaveTitle).isEqualTo(autoSaveTitle)
    }

    @Test
    fun `sets and reads content correctly`() {
        val post = PostModel()
        val content = "content"

        editPostRepository.setPost(post)
        editPostRepository.content = content

        assertThat(editPostRepository.getPost()!!.content).isEqualTo(content)
        assertThat(editPostRepository.content).isEqualTo(content)
    }

    @Test
    fun `sets and reads autoSaveContent correctly`() {
        val post = PostModel()
        val autoSaveContent = "autoSaveContent"

        editPostRepository.setPost(post)
        editPostRepository.autoSaveContent = autoSaveContent

        assertThat(editPostRepository.getPost()!!.autoSaveContent).isEqualTo(autoSaveContent)
        assertThat(editPostRepository.autoSaveContent).isEqualTo(autoSaveContent)
    }

    @Test
    fun `sets and reads excerpt correctly`() {
        val post = PostModel()
        val excerpt = "excerpt"

        editPostRepository.setPost(post)
        editPostRepository.excerpt = excerpt

        assertThat(editPostRepository.getPost()!!.excerpt).isEqualTo(excerpt)
        assertThat(editPostRepository.excerpt).isEqualTo(excerpt)
    }

    @Test
    fun `sets and reads autoSaveExcerpt correctly`() {
        val post = PostModel()
        val autoSaveExcerpt = "autoSaveExcerpt"

        editPostRepository.setPost(post)
        editPostRepository.autoSaveExcerpt = autoSaveExcerpt

        assertThat(editPostRepository.getPost()!!.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
        assertThat(editPostRepository.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
    }

    @Test
    fun `sets and reads password correctly`() {
        val post = PostModel()
        val password = "password"

        editPostRepository.setPost(post)
        editPostRepository.password = password

        assertThat(editPostRepository.getPost()!!.password).isEqualTo(password)
        assertThat(editPostRepository.password).isEqualTo(password)
    }

    @Test
    fun `sets and reads status correctly`() {
        val post = PostModel()
        val status = DRAFT

        editPostRepository.setPost(post)
        editPostRepository.status = status

        assertThat(editPostRepository.getPost()!!.status).isEqualTo(status.toString())
        assertThat(editPostRepository.status).isEqualTo(status)
    }

    @Test
    fun `sets and reads isPage correctly`() {
        val post = PostModel()
        val isPage = true

        editPostRepository.setPost(post)
        editPostRepository.isPage = isPage

        assertThat(editPostRepository.getPost()!!.isPage).isEqualTo(isPage)
        assertThat(editPostRepository.isPage).isEqualTo(isPage)
    }

    @Test
    fun `sets and reads isLocalDraft correctly`() {
        val post = PostModel()
        val isLocalDraft = true

        editPostRepository.setPost(post)
        editPostRepository.isLocalDraft = isLocalDraft

        assertThat(editPostRepository.getPost()!!.isLocalDraft).isEqualTo(isLocalDraft)
        assertThat(editPostRepository.isLocalDraft).isEqualTo(isLocalDraft)
    }

    @Test
    fun `sets and reads isLocallyChanged correctly`() {
        val post = PostModel()
        val isLocallyChanged = true

        editPostRepository.setPost(post)
        editPostRepository.isLocallyChanged = isLocallyChanged

        assertThat(editPostRepository.getPost()!!.isLocallyChanged).isEqualTo(isLocallyChanged)
        assertThat(editPostRepository.isLocallyChanged).isEqualTo(isLocallyChanged)
    }

    @Test
    fun `sets and reads featuredImageId correctly`() {
        val post = PostModel()
        val featuredImageId = 10L

        editPostRepository.setPost(post)
        editPostRepository.featuredImageId = featuredImageId

        assertThat(editPostRepository.getPost()!!.featuredImageId).isEqualTo(featuredImageId)
        assertThat(editPostRepository.featuredImageId).isEqualTo(featuredImageId)
    }

    @Test
    fun `sets and reads dateCreated correctly`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"

        editPostRepository.setPost(post)
        editPostRepository.dateCreated = dateCreated

        assertThat(editPostRepository.getPost()!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `sets and reads changesConfirmedContentHashcode correctly`() {
        val post = PostModel()
        val changesConfirmedContentHashcode = 10

        editPostRepository.setPost(post)
        editPostRepository.changesConfirmedContentHashcode = changesConfirmedContentHashcode

        assertThat(editPostRepository.getPost()!!.changesConfirmedContentHashcode).isEqualTo(
                changesConfirmedContentHashcode
        )
        assertThat(editPostRepository.changesConfirmedContentHashcode).isEqualTo(
                changesConfirmedContentHashcode
        )
    }

    @Test
    fun `sets and reads postFormat correctly`() {
        val post = PostModel()
        val postFormat = "format"

        editPostRepository.setPost(post)
        editPostRepository.postFormat = postFormat

        assertThat(editPostRepository.getPost()!!.postFormat).isEqualTo(postFormat)
        assertThat(editPostRepository.postFormat).isEqualTo(postFormat)
    }

    @Test
    fun `sets and reads slug correctly`() {
        val post = PostModel()
        val slug = "slug"

        editPostRepository.setPost(post)
        editPostRepository.slug = slug

        assertThat(editPostRepository.getPost()!!.slug).isEqualTo(slug)
        assertThat(editPostRepository.slug).isEqualTo(slug)
    }

    @Test
    fun `sets and reads link correctly`() {
        val post = PostModel()
        val link = "link"

        editPostRepository.setPost(post)
        editPostRepository.link = link

        assertThat(editPostRepository.getPost()!!.link).isEqualTo(link)
        assertThat(editPostRepository.link).isEqualTo(link)
    }

    @Test
    fun `sets and reads location correctly`() {
        val post = PostModel()
        val location = PostLocation(20.0, 30.0)

        editPostRepository.setPost(post)
        editPostRepository.location = location

        assertThat(editPostRepository.getPost()!!.location).isEqualTo(location)
        assertThat(editPostRepository.location).isEqualTo(location)
        assertThat(editPostRepository.hasLocation()).isTrue()
    }

    @Test
    fun `clears location correctly`() {
        val post = PostModel()
        val location = PostLocation(20.0, 30.0)
        val removedLocation = PostLocation(8888.0, 8888.0)

        editPostRepository.setPost(post)
        editPostRepository.location = location

        editPostRepository.clearLocation()

        assertThat(editPostRepository.getPost()!!.location).isEqualTo(removedLocation)
        assertThat(editPostRepository.location).isEqualTo(removedLocation)
        assertThat(editPostRepository.hasLocation()).isFalse()
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

        editPostRepository.setPost(post)
        editPostRepository.status = DRAFT
        editPostRepository.dateCreated = dateCreated
        whenever(postUtils.shouldPublishImmediately(DRAFT, dateCreated)).thenReturn(true)

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately()

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

        editPostRepository.setPost(post)
        editPostRepository.status = PUBLISHED
        editPostRepository.dateCreated = dateCreated
        whenever(postUtils.shouldPublishImmediately(PUBLISHED, dateCreated)).thenReturn(false)

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately()

        assertThat(editPostRepository.getPost()!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `updates post title if different`() {
        val post = PostModel()
        val title = "title"
        val updatedTitle = "updatedTitle"

        editPostRepository.setPost(post)
        editPostRepository.title = title

        val result = editPostRepository.updatePostTitleIfDifferent(updatedTitle)

        assertThat(result).isTrue()
        assertThat(editPostRepository.getPost()!!.title).isEqualTo(updatedTitle)
        assertThat(editPostRepository.title).isEqualTo(updatedTitle)
    }

    @Test
    fun `does not update post title if not different`() {
        val post = PostModel()
        val title = "title"

        editPostRepository.setPost(post)
        editPostRepository.title = title

        val result = editPostRepository.updatePostTitleIfDifferent(title)

        assertThat(result).isFalse()
        assertThat(editPostRepository.getPost()!!.title).isEqualTo(title)
        assertThat(editPostRepository.title).isEqualTo(title)
    }

    @Test
    fun `is not publishable when isPublishable(post) is false`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        whenever(postUtils.isPublishable(post)).thenReturn(false)

        assertThat(editPostRepository.isPublishable()).isFalse()
    }

    @Test
    fun `is publishable when isPublishable(post) is true`() {
        val post = PostModel()

        editPostRepository.setPost(post)

        whenever(postUtils.isPublishable(post)).thenReturn(true)

        assertThat(editPostRepository.isPublishable()).isTrue()
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

        editPostRepository.updateStatusFromSnapshot()

        assertThat(editPostRepository.status).isEqualTo(PUBLISHED)
    }
}
