package org.wordpress.android.ui.mysite

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.CategoryHeader
import org.wordpress.android.ui.mysite.MySiteItem.ListItem
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

val onClick = ListItemInteraction.create { }
val jetpackHeader = CategoryHeader(
        UiStringRes(R.string.my_site_header_jetpack)
)
val publishHeader = CategoryHeader(UiStringRes(R.string.my_site_header_publish))
val lookAndFeelHeader = CategoryHeader(
        UiStringRes(R.string.my_site_header_look_and_feel)
)
val configurationHeader = CategoryHeader(
        UiStringRes(R.string.my_site_header_configuration)
)
val externalHeader = CategoryHeader(UiStringRes(R.string.my_site_header_external))
val planItem = ListItem(
        R.drawable.ic_plans_white_24dp,
        UiStringRes(R.string.plan),
        secondaryText = UiStringText("plan"),
        onClick = onClick
)
val jetpackItem = ListItem(
        R.drawable.ic_cog_white_24dp,
        UiStringRes(R.string.my_site_btn_jetpack_settings),
        onClick = onClick
)
val activityItem = ListItem(
        R.drawable.ic_history_alt_white_24dp,
        UiStringRes(R.string.activity),
        onClick = onClick
)
val scanItem = ListItem(
        R.drawable.ic_scan_alt_white_24dp,
        UiStringRes(R.string.scan),
        onClick = onClick
)
val pagesItem = ListItem(
        R.drawable.ic_pages_white_24dp,
        UiStringRes(R.string.my_site_btn_site_pages),
        onClick = onClick
)
val postsItem = ListItem(
        R.drawable.ic_posts_white_24dp,
        UiStringRes(R.string.my_site_btn_blog_posts),
        onClick = onClick
)
val mediaItem = ListItem(
        R.drawable.ic_media_white_24dp,
        UiStringRes(R.string.media),
        onClick = onClick
)
val commentsItem = ListItem(
        R.drawable.ic_comment_white_24dp,
        UiStringRes(R.string.my_site_btn_comments),
        onClick = onClick
)
val adminItem = ListItem(
        R.drawable.ic_my_sites_white_24dp,
        UiStringRes(R.string.my_site_btn_view_admin),
        secondaryIcon = R.drawable.ic_external_white_24dp,
        onClick = onClick
)
val peopleItem = ListItem(
        R.drawable.ic_user_white_24dp,
        UiStringRes(R.string.people),
        onClick = onClick
)
val pluginsItem = ListItem(
        R.drawable.ic_plugins_white_24dp,
        UiStringRes(R.string.my_site_btn_plugins),
        onClick = onClick
)
val sharingItem = ListItem(
        R.drawable.ic_share_white_24dp,
        UiStringRes(R.string.my_site_btn_sharing),
        onClick = onClick
)
val siteSettingsItem = ListItem(
        R.drawable.ic_cog_white_24dp,
        UiStringRes(R.string.my_site_btn_site_settings),
        onClick = onClick
)
val themesItem = ListItem(
        R.drawable.ic_themes_white_24dp,
        UiStringRes(R.string.themes),
        onClick = onClick
)
val viewSiteItem = ListItem(
        R.drawable.ic_globe_white_24dp,
        UiStringRes(R.string.my_site_btn_view_site),
        secondaryIcon = R.drawable.ic_external_white_24dp,
        onClick = onClick
)
