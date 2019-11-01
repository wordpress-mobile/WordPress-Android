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
    private lateinit var editPostRepository: EditPostRepository
    @Before
    fun setUp() {
        editPostRepository = EditPostRepository(localeManager)
    }

    @Test
    fun `sets and reads post correctly`() {
        val post = PostModel()

        editPostRepository.post = post

        assertThat(editPostRepository.post).isEqualTo(post)
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

        editPostRepository.post = post
        editPostRepository.id = id

        assertThat(editPostRepository.post!!.id).isEqualTo(id)
        assertThat(editPostRepository.id).isEqualTo(id)
    }

    @Test
    fun `sets and reads localSiteId correctly`() {
        val post = PostModel()
        val localSiteId = 10

        editPostRepository.post = post
        editPostRepository.localSiteId = localSiteId

        assertThat(editPostRepository.post!!.localSiteId).isEqualTo(localSiteId)
        assertThat(editPostRepository.localSiteId).isEqualTo(localSiteId)
    }

    @Test
    fun `sets and reads remotePostId correctly`() {
        val post = PostModel()
        val remotePostId = 10L

        editPostRepository.post = post
        editPostRepository.remotePostId = remotePostId

        assertThat(editPostRepository.post!!.remotePostId).isEqualTo(remotePostId)
        assertThat(editPostRepository.remotePostId).isEqualTo(remotePostId)
    }

    @Test
    fun `sets and reads title correctly`() {
        val post = PostModel()
        val title = "title"

        editPostRepository.post = post
        editPostRepository.title = title

        assertThat(editPostRepository.post!!.title).isEqualTo(title)
        assertThat(editPostRepository.title).isEqualTo(title)
    }

    @Test
    fun `sets and reads autoSaveTitle correctly`() {
        val post = PostModel()
        val autoSaveTitle = "autoSaveTitle"

        editPostRepository.post = post
        editPostRepository.autoSaveTitle = autoSaveTitle

        assertThat(editPostRepository.post!!.autoSaveTitle).isEqualTo(autoSaveTitle)
        assertThat(editPostRepository.autoSaveTitle).isEqualTo(autoSaveTitle)
    }

    @Test
    fun `sets and reads content correctly`() {
        val post = PostModel()
        val content = "content"

        editPostRepository.post = post
        editPostRepository.content = content

        assertThat(editPostRepository.post!!.content).isEqualTo(content)
        assertThat(editPostRepository.content).isEqualTo(content)
    }

    @Test
    fun `sets and reads autoSaveContent correctly`() {
        val post = PostModel()
        val autoSaveContent = "autoSaveContent"

        editPostRepository.post = post
        editPostRepository.autoSaveContent = autoSaveContent

        assertThat(editPostRepository.post!!.autoSaveContent).isEqualTo(autoSaveContent)
        assertThat(editPostRepository.autoSaveContent).isEqualTo(autoSaveContent)
    }

    @Test
    fun `sets and reads excerpt correctly`() {
        val post = PostModel()
        val excerpt = "excerpt"

        editPostRepository.post = post
        editPostRepository.excerpt = excerpt

        assertThat(editPostRepository.post!!.excerpt).isEqualTo(excerpt)
        assertThat(editPostRepository.excerpt).isEqualTo(excerpt)
    }

    @Test
    fun `sets and reads autoSaveExcerpt correctly`() {
        val post = PostModel()
        val autoSaveExcerpt = "autoSaveExcerpt"

        editPostRepository.post = post
        editPostRepository.autoSaveExcerpt = autoSaveExcerpt

        assertThat(editPostRepository.post!!.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
        assertThat(editPostRepository.autoSaveExcerpt).isEqualTo(autoSaveExcerpt)
    }

    @Test
    fun `sets and reads password correctly`() {
        val post = PostModel()
        val password = "password"

        editPostRepository.post = post
        editPostRepository.password = password

        assertThat(editPostRepository.post!!.password).isEqualTo(password)
        assertThat(editPostRepository.password).isEqualTo(password)
    }

    @Test
    fun `sets and reads status correctly`() {
        val post = PostModel()
        val status = DRAFT

        editPostRepository.post = post
        editPostRepository.status = status

        assertThat(editPostRepository.post!!.status).isEqualTo(status.toString())
        assertThat(editPostRepository.status).isEqualTo(status)
    }

    @Test
    fun `sets and reads isPage correctly`() {
        val post = PostModel()
        val isPage = true

        editPostRepository.post = post
        editPostRepository.isPage = isPage

        assertThat(editPostRepository.post!!.isPage).isEqualTo(isPage)
        assertThat(editPostRepository.isPage).isEqualTo(isPage)
    }

    @Test
    fun `sets and reads isLocalDraft correctly`() {
        val post = PostModel()
        val isLocalDraft = true

        editPostRepository.post = post
        editPostRepository.isLocalDraft = isLocalDraft

        assertThat(editPostRepository.post!!.isLocalDraft).isEqualTo(isLocalDraft)
        assertThat(editPostRepository.isLocalDraft).isEqualTo(isLocalDraft)
    }

    @Test
    fun `sets and reads isLocallyChanged correctly`() {
        val post = PostModel()
        val isLocallyChanged = true

        editPostRepository.post = post
        editPostRepository.isLocallyChanged = isLocallyChanged

        assertThat(editPostRepository.post!!.isLocallyChanged).isEqualTo(isLocallyChanged)
        assertThat(editPostRepository.isLocallyChanged).isEqualTo(isLocallyChanged)
    }

    @Test
    fun `sets and reads featuredImageId correctly`() {
        val post = PostModel()
        val featuredImageId = 10L

        editPostRepository.post = post
        editPostRepository.featuredImageId = featuredImageId

        assertThat(editPostRepository.post!!.featuredImageId).isEqualTo(featuredImageId)
        assertThat(editPostRepository.featuredImageId).isEqualTo(featuredImageId)
    }

    @Test
    fun `sets and reads dateCreated correctly`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"

        editPostRepository.post = post
        editPostRepository.dateCreated = dateCreated

        assertThat(editPostRepository.post!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `sets and reads changesConfirmedContentHashcode correctly`() {
        val post = PostModel()
        val changesConfirmedContentHashcode = 10

        editPostRepository.post = post
        editPostRepository.changesConfirmedContentHashcode = changesConfirmedContentHashcode

        assertThat(editPostRepository.post!!.changesConfirmedContentHashcode).isEqualTo(
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

        editPostRepository.post = post
        editPostRepository.postFormat = postFormat

        assertThat(editPostRepository.post!!.postFormat).isEqualTo(postFormat)
        assertThat(editPostRepository.postFormat).isEqualTo(postFormat)
    }

    @Test
    fun `sets and reads slug correctly`() {
        val post = PostModel()
        val slug = "slug"

        editPostRepository.post = post
        editPostRepository.slug = slug

        assertThat(editPostRepository.post!!.slug).isEqualTo(slug)
        assertThat(editPostRepository.slug).isEqualTo(slug)
    }

    @Test
    fun `sets and reads link correctly`() {
        val post = PostModel()
        val link = "link"

        editPostRepository.post = post
        editPostRepository.link = link

        assertThat(editPostRepository.post!!.link).isEqualTo(link)
        assertThat(editPostRepository.link).isEqualTo(link)
    }

    @Test
    fun `sets and reads location correctly`() {
        val post = PostModel()
        val location = PostLocation(20.0, 30.0)

        editPostRepository.post = post
        editPostRepository.location = location

        assertThat(editPostRepository.post!!.location).isEqualTo(location)
        assertThat(editPostRepository.location).isEqualTo(location)
        assertThat(editPostRepository.hasLocation()).isTrue()
    }

    @Test
    fun `clears location correctly`() {
        val post = PostModel()
        val location = PostLocation(20.0, 30.0)
        val removedLocation = PostLocation(8888.0, 8888.0)

        editPostRepository.post = post
        editPostRepository.location = location

        editPostRepository.clearLocation()

        assertThat(editPostRepository.post!!.location).isEqualTo(removedLocation)
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

        editPostRepository.post = post
        editPostRepository.status = DRAFT
        editPostRepository.dateCreated = dateCreated

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately()

        assertThat(editPostRepository.post!!.dateCreated).isEqualTo(now)
        assertThat(editPostRepository.dateCreated).isEqualTo(now)
    }

    @Test
    fun `does not update publish date when not a draft`() {
        val post = PostModel()
        val dateCreated = "2019-05-05T14:33:20+0000"
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 1572000000000
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        editPostRepository.post = post
        editPostRepository.status = PUBLISHED
        editPostRepository.dateCreated = dateCreated

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately()

        assertThat(editPostRepository.post!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `does not update publish date when publish date in future`() {
        val post = PostModel()
        val dateCreated = "2019-11-05T14:33:20+0000"
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = 1572000000000
        calendar.timeZone = TimeZone.getTimeZone("UTC")

        editPostRepository.post = post
        editPostRepository.status = DRAFT
        editPostRepository.dateCreated = dateCreated

        editPostRepository.updatePublishDateIfShouldBePublishedImmediately()

        assertThat(editPostRepository.post!!.dateCreated).isEqualTo(dateCreated)
        assertThat(editPostRepository.dateCreated).isEqualTo(dateCreated)
    }

    @Test
    fun `updates post title if different`() {
        val post = PostModel()
        val title = "title"
        val updatedTitle = "updatedTitle"

        editPostRepository.post = post
        editPostRepository.title = title

        val result = editPostRepository.updatePostTitleIfDifferent(updatedTitle)

        assertThat(result).isTrue()
        assertThat(editPostRepository.post!!.title).isEqualTo(updatedTitle)
        assertThat(editPostRepository.title).isEqualTo(updatedTitle)
    }

    @Test
    fun `does not update post title if not different`() {
        val post = PostModel()
        val title = "title"

        editPostRepository.post = post
        editPostRepository.title = title

        val result = editPostRepository.updatePostTitleIfDifferent(title)

        assertThat(result).isFalse()
        assertThat(editPostRepository.post!!.title).isEqualTo(title)
        assertThat(editPostRepository.title).isEqualTo(title)
    }

    @Test
    fun `is not publishable when title, content and excerpt are empty`() {
        val post = PostModel()

        editPostRepository.post = post
        editPostRepository.title = ""
        editPostRepository.content = ""
        editPostRepository.excerpt = ""

        assertThat(editPostRepository.isPublishable()).isFalse()
    }

    @Test
    fun `is publishable when title is not empty`() {
        val post = PostModel()

        editPostRepository.post = post
        editPostRepository.title = "title"
        editPostRepository.content = ""
        editPostRepository.excerpt = ""

        assertThat(editPostRepository.isPublishable()).isTrue()
    }

    @Test
    fun `is publishable when content is not empty`() {
        val post = PostModel()

        editPostRepository.post = post
        editPostRepository.title = ""
        editPostRepository.content = "content"
        editPostRepository.excerpt = ""

        assertThat(editPostRepository.isPublishable()).isTrue()
    }

    @Test
    fun `is publishable when excerpt is not empty`() {
        val post = PostModel()

        editPostRepository.post = post
        editPostRepository.title = ""
        editPostRepository.content = ""
        editPostRepository.excerpt = "excerpt"

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

        editPostRepository.post = firstPost

        assertThat(editPostRepository.post).isEqualTo(firstPost)

        editPostRepository.saveForUndo()

        editPostRepository.post = secondPost

        assertThat(editPostRepository.post).isEqualTo(secondPost)

        editPostRepository.undo()

        assertThat(editPostRepository.post).isEqualTo(firstPost)
    }

    @Test
    fun `saves snapshot`() {
        val firstPost = PostModel()
        val firstPostId = 1
        firstPost.id = firstPostId

        editPostRepository.post = firstPost

        assertThat(editPostRepository.post).isEqualTo(firstPost)

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

        editPostRepository.post = firstPost

        assertThat(editPostRepository.post).isEqualTo(firstPost)

        editPostRepository.saveSnapshot()

        assertThat(editPostRepository.isSnapshotDifferent()).isFalse()

        editPostRepository.post = secondPost

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

        editPostRepository.post = firstPost

        editPostRepository.saveSnapshot()

        editPostRepository.post = secondPost

        assertThat(editPostRepository.status).isEqualTo(PENDING)

        editPostRepository.updateStatusFromSnapshot()

        assertThat(editPostRepository.status).isEqualTo(PUBLISHED)
    }
}
