package org.wordpress.android.ui.mysite

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

val onClick = ListItemInteraction.create { }
val jetpackHeader = CategoryHeader(
        UiStringRes(string.my_site_header_jetpack)
)
val publishHeader = CategoryHeader(UiStringRes(string.my_site_header_publish))
val lookAndFeelHeader = CategoryHeader(
        UiStringRes(string.my_site_header_look_and_feel)
)
val configurationHeader = CategoryHeader(
        UiStringRes(string.my_site_header_configuration)
)
val externalHeader = CategoryHeader(UiStringRes(string.my_site_header_external))
val planItem = ListItem(
        drawable.ic_plans_white_24dp,
        UiStringRes(string.plan),
        secondaryText = UiStringText("plan"),
        onClick = onClick
)
val activityItem = ListItem(
        drawable.ic_history_alt_white_24dp,
        UiStringRes(string.activity),
        onClick = onClick
)
val scanItem = ListItem(
        drawable.ic_scan_alt_white_24dp,
        UiStringRes(string.scan),
        onClick = onClick
)
val pagesItem = ListItem(
        drawable.ic_pages_white_24dp,
        UiStringRes(string.my_site_btn_site_pages),
        onClick = onClick
)
val postsItem = ListItem(
        drawable.ic_posts_white_24dp,
        UiStringRes(string.my_site_btn_blog_posts),
        onClick = onClick
)
val mediaItem = ListItem(
        drawable.ic_media_white_24dp,
        UiStringRes(string.media),
        onClick = onClick
)
val commentsItem = ListItem(
        drawable.ic_comment_white_24dp,
        UiStringRes(string.my_site_btn_comments),
        onClick = onClick
)
val adminItem = ListItem(
        drawable.ic_my_sites_white_24dp,
        UiStringRes(string.my_site_btn_view_admin),
        secondaryIcon = drawable.ic_external_white_24dp,
        onClick = onClick
)
val peopleItem = ListItem(
        drawable.ic_user_white_24dp,
        UiStringRes(string.people),
        onClick = onClick
)
val pluginsItem = ListItem(
        drawable.ic_plugins_white_24dp,
        UiStringRes(string.my_site_btn_plugins),
        onClick = onClick
)
val sharingItem = ListItem(
        drawable.ic_share_white_24dp,
        UiStringRes(string.my_site_btn_sharing),
        onClick = onClick
)
val siteSettingsItem = ListItem(
        drawable.ic_cog_white_24dp,
        UiStringRes(string.my_site_btn_site_settings),
        onClick = onClick
)
val themesItem = ListItem(
        drawable.ic_themes_white_24dp,
        UiStringRes(string.themes),
        onClick = onClick
)
val viewSiteItem = ListItem(
        drawable.ic_globe_white_24dp,
        UiStringRes(string.my_site_btn_view_site),
        secondaryIcon = drawable.ic_external_white_24dp,
        onClick = onClick
)
