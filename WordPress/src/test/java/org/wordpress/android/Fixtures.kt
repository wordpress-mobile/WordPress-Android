package org.wordpress.android

import org.wordpress.android.models.CategoryModel
import org.wordpress.android.models.SiteSettingsModel

object Fixtures {
    fun createFakeSiteSettingsModel(): SiteSettingsModel {
        val siteSettingsModel = SiteSettingsModel()

        siteSettingsModel.isInLocalTable = true
        siteSettingsModel.hasVerifiedCredentials = true
        siteSettingsModel.localTableId = 1
        siteSettingsModel.address = "address"
        siteSettingsModel.username = "username"
        siteSettingsModel.password = "password"
        siteSettingsModel.title = "title"
        siteSettingsModel.tagline = "tagline"
        siteSettingsModel.language = "language"
        siteSettingsModel.siteIconMediaId = 1
        siteSettingsModel.languageId = 1
        siteSettingsModel.privacy = 1
        siteSettingsModel.location = true
        siteSettingsModel.defaultCategory = 1
        siteSettingsModel.categories = arrayOf(CategoryModel())
        siteSettingsModel.defaultPostFormat = "defaultPostFormat"
        siteSettingsModel.postFormats = mapOf("key" to "value").toMutableMap()
        siteSettingsModel.showRelatedPosts = true
        siteSettingsModel.showRelatedPostHeader = true
        siteSettingsModel.showRelatedPostImages = true
        siteSettingsModel.allowComments = true
        siteSettingsModel.sendPingbacks = true
        siteSettingsModel.receivePingbacks = true
        siteSettingsModel.shouldCloseAfter = true
        siteSettingsModel.closeCommentAfter = 1
        siteSettingsModel.sortCommentsBy = 1
        siteSettingsModel.shouldThreadComments = true
        siteSettingsModel.threadingLevels = 1
        siteSettingsModel.shouldPageComments = true
        siteSettingsModel.commentsPerPage = 1
        siteSettingsModel.commentApprovalRequired = true
        siteSettingsModel.commentsRequireIdentity = true
        siteSettingsModel.commentsRequireUserAccount = true
        siteSettingsModel.commentAutoApprovalKnownUsers = true
        siteSettingsModel.maxLinks = 1
        siteSettingsModel.holdForModeration = listOf("holdForModeration")
        siteSettingsModel.blacklist = listOf("blacklist")
        siteSettingsModel.sharingLabel = "sharingLabel"
        siteSettingsModel.sharingButtonStyle = "sharingButtonStyle"
        siteSettingsModel.allowReblogButton = true
        siteSettingsModel.allowLikeButton = true
        siteSettingsModel.allowCommentLikes = true
        siteSettingsModel.twitterUsername = "twitterUsername"
        siteSettingsModel.startOfWeek = "startOfWeek"
        siteSettingsModel.dateFormat = "dateFormat"
        siteSettingsModel.timeFormat = "timeFormat"
        siteSettingsModel.timezone = "timezone"
        siteSettingsModel.postsPerPage = 1
        siteSettingsModel.ampSupported = true
        siteSettingsModel.ampEnabled = true
        siteSettingsModel.quotaDiskSpace = "quotaDiskSpace"
        siteSettingsModel.isPortfolioEnabled = true
        siteSettingsModel.portfolioPostsPerPage = 1
        return siteSettingsModel
    }
}
