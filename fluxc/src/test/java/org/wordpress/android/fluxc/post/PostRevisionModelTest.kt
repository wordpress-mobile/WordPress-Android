package org.wordpress.android.fluxc.post

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.revisions.Diff
import org.wordpress.android.fluxc.model.revisions.DiffOperations
import org.wordpress.android.fluxc.model.revisions.LocalDiffModel
import org.wordpress.android.fluxc.model.revisions.LocalDiffType
import org.wordpress.android.fluxc.model.revisions.LocalRevisionModel
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class PostRevisionModelTest {
    @Test
    fun testSampleRevisionModel() {
        val revision = PostTestUtils.generateSamplePostRevision()
        assertNotNull(revision)
        assertEquals(1, revision.revisionId)
        assertEquals(2, revision.diffFromVersion)
        assertEquals(5, revision.totalAdditions)
        assertEquals(6, revision.totalDeletions)
        assertEquals("post content", revision.postContent)
        assertEquals("post excerpt", revision.postExcerpt)
        assertEquals("post title", revision.postTitle)
        assertEquals("2018-09-04 12:19:34Z", revision.postDateGmt)
        assertEquals("2018-09-05 13:19:34Z", revision.postModifiedGmt)
        assertEquals("111111111", revision.postAuthorId)

        val titleDiffs = revision.titleDiffs
        assertNotNull(titleDiffs)
        assertEquals(5, titleDiffs.size)

        assertEquals(DiffOperations.COPY, titleDiffs[0].operation)
        assertEquals("copy title", titleDiffs[0].value)

        assertEquals(DiffOperations.COPY, titleDiffs[1].operation)
        assertEquals("copy another title", titleDiffs[1].value)

        assertEquals(DiffOperations.ADD, titleDiffs[2].operation)
        assertEquals("add new title", titleDiffs[2].value)

        assertEquals(DiffOperations.DELETE, titleDiffs[3].operation)
        assertEquals("del title", titleDiffs[3].value)

        assertEquals(DiffOperations.ADD, titleDiffs[4].operation)
        assertEquals("add different title", titleDiffs[4].value)

        val contentDiffs = revision.contentDiffs
        assertNotNull(contentDiffs)
        assertEquals(3, contentDiffs.size)

        assertEquals(DiffOperations.COPY, contentDiffs[0].operation)
        assertEquals("copy some content", contentDiffs[0].value)

        assertEquals(DiffOperations.ADD, contentDiffs[1].operation)
        assertEquals("add new content", contentDiffs[1].value)

        assertEquals(DiffOperations.DELETE, contentDiffs[2].operation)
        assertEquals("del all the content", contentDiffs[2].value)
    }

    @Test
    fun testRevisionModelEquals() {
        val sampleRevision1 = PostTestUtils.generateSamplePostRevision()
        val sampleRevision2 = PostTestUtils.generateSamplePostRevision()

        assertEquals(sampleRevision1, sampleRevision2)
        assertEquals(sampleRevision1.hashCode(), sampleRevision2.hashCode())

        sampleRevision1.titleDiffs[0] = Diff(DiffOperations.COPY, "wrong value")
        assertNotEquals(sampleRevision1, sampleRevision2)
        assertNotEquals(sampleRevision1.hashCode(), sampleRevision2.hashCode())
    }

    @Test
    fun testRevisionToLocalRevision() {
        val sampleRevision = PostTestUtils.generateSamplePostRevision()
        val postModel = PostTestUtils.generateSampleLocalDraftPost()

        val site = SiteModel()
        site.siteId = 77

        val localRevisionModel = LocalRevisionModel.fromRevisionModel(sampleRevision, site, postModel)

        assertNotNull(localRevisionModel)
        assertEquals(sampleRevision.revisionId, localRevisionModel.revisionId)
        assertEquals(site.siteId, localRevisionModel.siteId)
        assertEquals(postModel.remotePostId, localRevisionModel.postId)
        assertEquals(sampleRevision.diffFromVersion, localRevisionModel.diffFromVersion)
        assertEquals(sampleRevision.totalAdditions, localRevisionModel.totalAdditions)
        assertEquals(sampleRevision.totalDeletions, localRevisionModel.totalDeletions)
        assertEquals(sampleRevision.postContent, localRevisionModel.postContent)
        assertEquals(sampleRevision.postExcerpt, localRevisionModel.postExcerpt)
        assertEquals(sampleRevision.postTitle, localRevisionModel.postTitle)
        assertEquals(sampleRevision.postDateGmt, localRevisionModel.postDateGmt)
        assertEquals(sampleRevision.postModifiedGmt, localRevisionModel.postModifiedGmt)
        assertEquals(sampleRevision.postAuthorId, localRevisionModel.postAuthorId)
    }

    @Test
    fun testDiffToLocalDiff() {
        val site = SiteModel()
        site.siteId = 77
        val postModel = PostTestUtils.generateSampleLocalDraftPost()

        val revision = PostTestUtils.generateSamplePostRevision()
        val diff = revision.titleDiffs[0]

        val localDiff = LocalDiffModel.fromDiffAndLocalRevision(
                diff, LocalDiffType.TITLE, LocalRevisionModel.fromRevisionModel(revision, site, postModel))

        assertNotNull(localDiff)
        assertEquals(diff.operation, DiffOperations.fromString(localDiff.operation))
        assertEquals(diff.value, localDiff.value)
    }
}
