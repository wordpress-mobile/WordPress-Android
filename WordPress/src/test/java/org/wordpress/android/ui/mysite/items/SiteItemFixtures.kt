package org.wordpress.android.ui.mysite.items

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.CategoryHeaderItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction.DOMAINS
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

val SITE_ITEM_ACTION: (ListItemAction) -> Unit = {}
val JETPACK_HEADER = CategoryHeaderItem(
    UiStringRes(R.string.my_site_header_jetpack)
)
val PUBLISH_HEADER = CategoryHeaderItem(UiStringRes(R.string.my_site_header_publish))
val LOOK_AND_FEEL_HEADER = CategoryHeaderItem(
    UiStringRes(R.string.my_site_header_look_and_feel)
)
val CONFIGURATION_HEADER = CategoryHeaderItem(
    UiStringRes(R.string.my_site_header_configuration)
)
val EXTERNAL_HEADER = CategoryHeaderItem(UiStringRes(R.string.my_site_header_external))
const val PLAN_NAME = "plan_name"
val PLAN_ITEM = ListItem(
    R.drawable.ic_plans_white_24dp,
    UiStringRes(R.string.plan),
    secondaryText = UiStringText(PLAN_NAME),
    onClick = ListItemInteraction.create(ListItemAction.PLAN, SITE_ITEM_ACTION)
)
val JETPACK_ITEM = ListItem(
    R.drawable.ic_cog_white_24dp,
    UiStringRes(R.string.my_site_btn_jetpack_settings),
    onClick = ListItemInteraction.create(ListItemAction.JETPACK_SETTINGS, SITE_ITEM_ACTION)
)
val STATS_ITEM = ListItem(
    R.drawable.ic_stats_alt_white_24dp,
    UiStringRes(R.string.stats),
    onClick = ListItemInteraction.create(ListItemAction.STATS, SITE_ITEM_ACTION)
)
val ACTIVITY_ITEM = ListItem(
    R.drawable.ic_gridicons_clipboard_white_24dp,
    UiStringRes(R.string.activity_log),
    onClick = ListItemInteraction.create(ListItemAction.ACTIVITY_LOG, SITE_ITEM_ACTION)
)
val BACKUP_ITEM = ListItem(
    R.drawable.ic_gridicons_cloud_upload_white_24dp,
    UiStringRes(R.string.backup),
    onClick = ListItemInteraction.create(ListItemAction.BACKUP, SITE_ITEM_ACTION)
)
val SCAN_ITEM = ListItem(
    R.drawable.ic_baseline_security_white_24dp,
    UiStringRes(R.string.scan),
    onClick = ListItemInteraction.create(ListItemAction.SCAN, SITE_ITEM_ACTION)
)
val PAGES_ITEM = ListItem(
    R.drawable.ic_pages_white_24dp,
    UiStringRes(R.string.my_site_btn_site_pages),
    onClick = ListItemInteraction.create(ListItemAction.PAGES, SITE_ITEM_ACTION)
)
val POSTS_ITEM = ListItem(
    R.drawable.ic_posts_white_24dp,
    UiStringRes(R.string.my_site_btn_blog_posts),
    onClick = ListItemInteraction.create(ListItemAction.POSTS, SITE_ITEM_ACTION)
)
val MEDIA_ITEM = ListItem(
    R.drawable.ic_media_white_24dp,
    UiStringRes(R.string.media),
    onClick = ListItemInteraction.create(ListItemAction.MEDIA, SITE_ITEM_ACTION)
)
val COMMENTS_ITEM = ListItem(
    R.drawable.ic_comment_white_24dp,
    UiStringRes(R.string.my_site_btn_comments),
    onClick = ListItemInteraction.create(ListItemAction.COMMENTS, SITE_ITEM_ACTION)
)
val ADMIN_ITEM = ListItem(
    R.drawable.ic_wordpress_white_24dp,
    UiStringRes(R.string.my_site_btn_view_admin),
    secondaryIcon = R.drawable.ic_external_white_24dp,
    onClick = ListItemInteraction.create(ListItemAction.ADMIN, SITE_ITEM_ACTION)
)
val PEOPLE_ITEM = ListItem(
    R.drawable.ic_user_white_24dp,
    UiStringRes(R.string.people),
    onClick = ListItemInteraction.create(ListItemAction.PEOPLE, SITE_ITEM_ACTION)
)
val PLUGINS_ITEM = ListItem(
    R.drawable.ic_plugins_white_24dp,
    UiStringRes(R.string.my_site_btn_plugins),
    onClick = ListItemInteraction.create(ListItemAction.PLUGINS, SITE_ITEM_ACTION)
)
val SHARING_ITEM = ListItem(
    R.drawable.ic_share_white_24dp,
    UiStringRes(R.string.my_site_btn_sharing),
    onClick = ListItemInteraction.create(ListItemAction.SHARING, SITE_ITEM_ACTION)
)
val SITE_SETTINGS_ITEM = ListItem(
    R.drawable.ic_cog_white_24dp,
    UiStringRes(R.string.my_site_btn_site_settings),
    onClick = ListItemInteraction.create(ListItemAction.SITE_SETTINGS, SITE_ITEM_ACTION)
)
val THEMES_ITEM = ListItem(
    R.drawable.ic_themes_white_24dp,
    UiStringRes(R.string.themes),
    onClick = ListItemInteraction.create(ListItemAction.THEMES, SITE_ITEM_ACTION)
)
val VIEW_SITE_ITEM = ListItem(
    R.drawable.ic_globe_white_24dp,
    UiStringRes(R.string.my_site_btn_view_site),
    secondaryIcon = R.drawable.ic_external_white_24dp,
    onClick = ListItemInteraction.create(ListItemAction.VIEW_SITE, SITE_ITEM_ACTION)
)
val DOMAINS_ITEM = ListItem(
    R.drawable.ic_domains_white_24dp,
    UiStringRes(R.string.my_site_btn_domains),
    onClick = ListItemInteraction.create(DOMAINS, SITE_ITEM_ACTION)
)
