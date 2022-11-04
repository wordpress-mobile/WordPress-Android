package org.wordpress.android.fluxc.common

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.comments.CommentsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentParent
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse
import org.wordpress.android.fluxc.persistence.comments.CommentEntityList
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.utils.DateTimeUtilsWrapper
import java.util.Date

class CommentsMapperTest {
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper = mock()
    private val mapper = CommentsMapper(dateTimeUtilsWrapper)

    @Test
    fun `xmlrpc dto is converted to entity`() {
        val comment = getDefaultComment(false).copy(
                authorProfileImageUrl = null,
                datePublished = "2021-07-29T21:29:27+00:00"
        )
        val site = SiteModel().apply {
            id = comment.localSiteId
            selfHostedSiteId = comment.remoteSiteId
        }
        val xmlRpcDto = comment.toXmlRpcDto()

        whenever(dateTimeUtilsWrapper.timestampFromIso8601(any())).thenReturn(comment.publishedTimestamp)
        whenever(dateTimeUtilsWrapper.iso8601UTCFromDate(any())).thenReturn(comment.datePublished)
        val mappedEntity = mapper.commentXmlRpcDTOToEntity(xmlRpcDto, site)

        assertThat(mappedEntity).isEqualTo(comment)
    }

    @Test
    fun `xmlrpc dto list is converted to entity list`() {
        val commentList = getDefaultCommentList(false).map { it.copy(id = 0, authorProfileImageUrl = null) }
        val site = SiteModel().apply {
            id = commentList.first().localSiteId
            selfHostedSiteId = commentList.first().remoteSiteId
        }
        val xmlRpcDtoList = commentList.map { it.toXmlRpcDto() }

        whenever(dateTimeUtilsWrapper.timestampFromIso8601(any())).thenReturn(commentList.first().publishedTimestamp)
        whenever(dateTimeUtilsWrapper.iso8601UTCFromDate(any())).thenReturn(commentList.first().datePublished)

        val mappedEntityList = mapper.commentXmlRpcDTOToEntityList(xmlRpcDtoList.toTypedArray(), site)

        assertThat(mappedEntityList).isEqualTo(commentList)
    }

    @Test
    fun `dto is converted to entity`() {
        val comment = getDefaultComment(true).copy(datePublished = "2021-07-29T21:29:27+00:00")
        val site = SiteModel().apply {
            id = comment.localSiteId
            siteId = comment.remoteSiteId
        }
        val commentDto = comment.toDto()

        whenever(dateTimeUtilsWrapper.timestampFromIso8601(any())).thenReturn(comment.publishedTimestamp)
        val mappedEntity = mapper.commentDtoToEntity(commentDto, site)

        assertThat(mappedEntity).isEqualTo(comment)
    }

    @Test
    fun `entity is converted to model`() {
        val comment = getDefaultComment(true).copy(datePublished = "2021-07-29T21:29:27+00:00")
        val commentModel = comment.toModel()

        val mappedModel = mapper.commentEntityToLegacyModel(comment)

        assertModelsEqual(mappedModel, commentModel)
    }

    @Test
    fun `model is converted to entity`() {
        val comment = getDefaultComment(true).copy(datePublished = "2021-07-29T21:29:27+00:00")
        val commentModel = comment.toModel()

        val mappedEntity = mapper.commentLegacyModelToEntity(commentModel)

        assertThat(mappedEntity).isEqualTo(comment)
    }

    @Suppress("ComplexMethod")
    private fun assertModelsEqual(mappedModel: CommentModel, commentModel: CommentModel): Boolean {
        return mappedModel.id == commentModel.id &&
        mappedModel.remoteCommentId == commentModel.remoteCommentId &&
        mappedModel.remotePostId == commentModel.remotePostId &&
        mappedModel.authorId == commentModel.authorId &&
        mappedModel.localSiteId == commentModel.localSiteId &&
        mappedModel.remoteSiteId == commentModel.remoteSiteId &&
        mappedModel.authorUrl == commentModel.authorUrl &&
        mappedModel.authorName == commentModel.authorName &&
        mappedModel.authorEmail == commentModel.authorEmail &&
        mappedModel.authorProfileImageUrl == commentModel.authorProfileImageUrl &&
        mappedModel.postTitle == commentModel.postTitle &&
        mappedModel.status == commentModel.status &&
        mappedModel.datePublished == commentModel.datePublished &&
        mappedModel.publishedTimestamp == commentModel.publishedTimestamp &&
        mappedModel.content == commentModel.content &&
        mappedModel.url == commentModel.url &&
        mappedModel.hasParent == commentModel.hasParent &&
        mappedModel.parentId == commentModel.parentId &&
        mappedModel.iLike == commentModel.iLike
    }

    private fun CommentEntity.toDto(): CommentWPComRestResponse {
        val entity = this
        return CommentWPComRestResponse().apply {
            ID = entity.remoteCommentId
            URL = entity.url
            author = Author().apply {
                ID = entity.authorId
                URL = entity.authorUrl
                avatar_URL = entity.authorProfileImageUrl
                email = entity.authorEmail
                name = entity.authorName
            }
            content = entity.content
            date = entity.datePublished
            i_like = entity.iLike
            parent = CommentParent().apply {
                ID = entity.parentId
            }
            post = Post().apply {
                type = "post"
                title = entity.postTitle
                link = "https://public-api.wordpress.com/rest/v1.1/sites/185464053/posts/85"
                ID = entity.remotePostId
            }
            status = entity.status
        }
    }

    private fun CommentEntity.toModel(): CommentModel {
        val entity = this
        return CommentModel().apply {
            id = entity.id.toInt()
            remoteCommentId = entity.remoteCommentId
            remotePostId = entity.remotePostId
            authorId = entity.authorId
            localSiteId = entity.localSiteId
            remoteSiteId = entity.remoteSiteId
            authorUrl = entity.authorUrl
            authorName = entity.authorName
            authorEmail = entity.authorEmail
            authorProfileImageUrl = entity.authorProfileImageUrl
            postTitle = entity.postTitle
            status = entity.status
            datePublished = entity.datePublished
            publishedTimestamp = entity.publishedTimestamp
            content = entity.content
            url = entity.authorProfileImageUrl
            hasParent = entity.hasParent
            parentId = entity.parentId
            iLike = entity.iLike
        }
    }

    private fun CommentEntity.toXmlRpcDto(): HashMap<*, *> {
        return hashMapOf<String, Any?>(
                "parent" to this.parentId.toString(),
                "post_title" to this.postTitle,
                "author" to this.authorName,
                "link" to this.url,
                "date_created_gmt" to Date(),
                "comment_id" to this.remoteCommentId.toString(),
                "content" to this.content,
                "author_url" to this.authorUrl,
                "post_id" to this.remotePostId,
                "user_id" to this.authorId,
                "author_email" to this.authorEmail,
                "status" to this.status
        )
    }

    private fun getDefaultComment(allowNulls: Boolean): CommentEntity {
        return CommentEntity(
                id = 0,
                remoteCommentId = 10,
                remotePostId = 100,
                authorId = 44,
                localSiteId = 10_000,
                remoteSiteId = 100_000,
                authorUrl = if (allowNulls) null else "https://test-debug-site.wordpress.com",
                authorName = if (allowNulls) null else "authorname",
                authorEmail = if (allowNulls) null else "email@wordpress.com",
                authorProfileImageUrl = if (allowNulls) null else "https://gravatar.com/avatar/111222333",
                postTitle = if (allowNulls) null else "again",
                status = APPROVED.toString(),
                datePublished = if (allowNulls) null else "2021-05-12T15:10:40+02:00",
                publishedTimestamp = 1_000_000,
                content = if (allowNulls) null else "content example",
                url = if (allowNulls) null else "https://test-debug-site.wordpress.com/2021/02/25/again/#comment-137",
                hasParent = true,
                parentId = 1_000L,
                iLike = false
        )
    }

    private fun getDefaultCommentList(allowNulls: Boolean): CommentEntityList {
        val comment = getDefaultComment(allowNulls)
        return listOf(
                comment.copy(id = 1, remoteCommentId = 10, datePublished = "2021-07-24T00:51:43+02:00"),
                comment.copy(id = 2, remoteCommentId = 20, datePublished = "2021-07-24T00:51:43+02:00"),
                comment.copy(id = 3, remoteCommentId = 30, datePublished = "2021-07-24T00:51:43+02:00"),
                comment.copy(id = 4, remoteCommentId = 40, datePublished = "2021-07-24T00:51:43+02:00")
        )
    }
}
