package org.wordpress.android.fluxc.model.comments

import dagger.Reusable
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.CommentModel
import org.wordpress.android.fluxc.model.CommentStatus
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import org.wordpress.android.fluxc.model.CommentStatus.SPAM
import org.wordpress.android.fluxc.model.CommentStatus.TRASH
import org.wordpress.android.fluxc.model.CommentStatus.UNAPPROVED
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.comment.CommentWPComRestResponse
import org.wordpress.android.fluxc.network.xmlrpc.XMLRPCUtils
import org.wordpress.android.fluxc.persistence.comments.CommentsDao.CommentEntity
import org.wordpress.android.fluxc.utils.DateTimeUtilsWrapper
import java.util.Date
import javax.inject.Inject

@Reusable
class CommentsMapper @Inject constructor(
    private val dateTimeUtilsWrapper: DateTimeUtilsWrapper
) {
    fun commentDtoToEntity(commentDto: CommentWPComRestResponse, site: SiteModel): CommentEntity {
        return CommentEntity(
            remoteCommentId = commentDto.ID,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            authorUrl = commentDto.author?.URL,
            authorName = commentDto.author?.name?.let {
                StringEscapeUtils.unescapeHtml4(it)
            },
            authorEmail = commentDto.author?.email?.let {
                if ("false".equals(it)) {
                    ""
                } else {
                    it
                }
            },
            authorProfileImageUrl = commentDto.author?.avatar_URL,
            remotePostId = commentDto.post?.ID ?: 0L,
            postTitle = StringEscapeUtils.unescapeHtml4(commentDto.post?.title),
            status = commentDto.status,
            datePublished = commentDto.date,
            publishedTimestamp = dateTimeUtilsWrapper.timestampFromIso8601(commentDto.date),
            content = commentDto.content,
            url = commentDto.URL,
            authorId = commentDto.author?.ID ?: 0L,
            hasParent = commentDto.parent != null,
            parentId = commentDto.parent?.ID ?: 0L,
            iLike = commentDto.i_like
        )
    }

    fun commentEntityToLegacyModel(entity: CommentEntity): CommentModel {
        return CommentModel().apply {
            this.id = entity.id.toInt()
            this.remoteCommentId = entity.remoteCommentId
            this.remotePostId = entity.remotePostId
            this.authorId = entity.authorId
            this.localSiteId = entity.localSiteId
            this.remoteSiteId = entity.remoteSiteId
            this.authorUrl = entity.authorUrl
            this.authorName = entity.authorName
            this.authorEmail = entity.authorEmail
            this.authorProfileImageUrl = entity.authorProfileImageUrl
            this.postTitle = entity.postTitle
            this.status = entity.status ?: ""
            this.datePublished = entity.datePublished ?: ""
            this.publishedTimestamp = entity.publishedTimestamp
            this.content = entity.content ?: ""
            this.url = entity.url ?: ""
            this.hasParent = entity.hasParent
            this.parentId = entity.parentId
            this.iLike = entity.iLike
        }
    }

    fun commentLegacyModelToEntity(commentModel: CommentModel): CommentEntity {
        return CommentEntity(
                id = commentModel.id.toLong(),
                remoteCommentId = commentModel.remoteCommentId,
                remotePostId = commentModel.remotePostId,
                authorId = commentModel.authorId,
                localSiteId = commentModel.localSiteId,
                remoteSiteId = commentModel.remoteSiteId,
                authorUrl = commentModel.authorUrl,
                authorName = commentModel.authorName,
                authorEmail = commentModel.authorEmail,
                authorProfileImageUrl = commentModel.authorProfileImageUrl,
                postTitle = commentModel.postTitle,
                status = commentModel.status,
                datePublished = commentModel.datePublished,
                publishedTimestamp = commentModel.publishedTimestamp,
                content = commentModel.content,
                url = commentModel.url,
                hasParent = commentModel.hasParent,
                parentId = commentModel.parentId,
                iLike = commentModel.iLike
        )
    }

    @Suppress("ForbiddenComment")
    fun commentXmlRpcDTOToEntity(commentObject: Any?, site: SiteModel): CommentEntity? {
        if (commentObject !is HashMap<*, *>) {
            return null
        }
        val commentMap: HashMap<*, *> = commentObject

        val datePublished = dateTimeUtilsWrapper.iso8601UTCFromDate(
                XMLRPCUtils.safeGetMapValue(commentMap, "date_created_gmt", Date())
        )

        val remoteParentCommentId = XMLRPCUtils.safeGetMapValue(commentMap, "parent", 0L)

        return CommentEntity(
                remoteCommentId = XMLRPCUtils.safeGetMapValue(commentMap, "comment_id", 0L),
                remotePostId = XMLRPCUtils.safeGetMapValue(commentMap, "post_id", 0L),
                authorId = XMLRPCUtils.safeGetMapValue(commentMap, "user_id", 0L),
                localSiteId = site.id,
                remoteSiteId = site.selfHostedSiteId,
                authorUrl = XMLRPCUtils.safeGetMapValue(commentMap, "author_url", ""),
                authorName = StringEscapeUtils.unescapeHtml4(
                        XMLRPCUtils.safeGetMapValue(commentMap, "author", "")
                ),
                authorEmail = XMLRPCUtils.safeGetMapValue(commentMap, "author_email", ""),
                // TODO: set authorProfileImageUrl - get the hash from the email address?
                authorProfileImageUrl = null,
                postTitle = StringEscapeUtils.unescapeHtml4(
                        XMLRPCUtils.safeGetMapValue(
                                commentMap,
                                "post_title", ""
                        )
                ),
                status = getCommentStatusFromXMLRPCStatusString(
                        XMLRPCUtils.safeGetMapValue(commentMap, "status", "approve")
                ).toString(),
                datePublished = datePublished,
                publishedTimestamp = dateTimeUtilsWrapper.timestampFromIso8601(datePublished),
                content = XMLRPCUtils.safeGetMapValue(commentMap, "content", ""),
                url = XMLRPCUtils.safeGetMapValue(commentMap, "link", ""),
                hasParent = remoteParentCommentId > 0,
                parentId = if (remoteParentCommentId > 0) remoteParentCommentId else 0L,
                iLike = false
        )
    }

    fun commentXmlRpcDTOToEntityList(response: Any?, site: SiteModel): List<CommentEntity> {
        val comments: MutableList<CommentEntity> = ArrayList()
        if (response !is Array<*>) {
            return comments
        }

        response.forEach { commentObject ->
            commentXmlRpcDTOToEntity(commentObject, site)?.let {
                comments.add(it)
            }
        }

        return comments
    }

    private fun getCommentStatusFromXMLRPCStatusString(stringStatus: String): CommentStatus {
        return when (stringStatus) {
            "approve" -> APPROVED
            "hold" -> UNAPPROVED
            "spam" -> SPAM
            "trash" -> TRASH
            else -> APPROVED
        }
    }
}
