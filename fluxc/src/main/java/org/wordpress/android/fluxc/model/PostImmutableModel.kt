package org.wordpress.android.fluxc.model

import org.json.JSONObject
import org.wordpress.android.fluxc.model.post.PostLocation

interface PostImmutableModel {
    val id: Int
    val localSiteId: Int
    val remoteSiteId: Long
    val remotePostId: Long
    val title: String
    val content: String
    val dateCreated: String
    val lastModified: String
    val remoteLastModified: String
    val categoryIds: String
    val categoryIdList: List<Long>
    val customFields: String
    val link: String
    val excerpt: String
    val tagNames: String
    val tagNameList: List<String>
    val status: String
    val password: String
    val featuredImageId: Long
    val postFormat: String
    val slug: String
    val longitude: Double
    val latitude: Double
    val location: PostLocation
    val authorId: Long
    val authorDisplayName: String?
    val changesConfirmedContentHashcode: Int
    val isPage: Boolean
    val parentId: Long
    val parentTitle: String
    val isLocalDraft: Boolean
    val isLocallyChanged: Boolean
    val autoSaveRevisionId: Long
    val autoSaveModified: String?
    val remoteAutoSaveModified: String?
    val autoSavePreviewUrl: String?
    val autoSaveTitle: String?
    val autoSaveContent: String?
    val autoSaveExcerpt: String?
    val hasCapabilityPublishPost: Boolean
    val hasCapabilityEditPost: Boolean
    val hasCapabilityDeletePost: Boolean
    val dateLocallyChanged: String

    fun hasFeaturedImage(): Boolean

    fun hasUnpublishedRevision(): Boolean

    fun contentHashcode(): Int

    fun getCustomField(key: String): JSONObject?

    fun supportsLocation(): Boolean

    fun hasLocation(): Boolean

    fun shouldDeleteLatitude(): Boolean

    fun shouldDeleteLongitude(): Boolean
}
