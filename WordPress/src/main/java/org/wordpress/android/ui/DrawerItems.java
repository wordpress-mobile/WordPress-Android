package org.wordpress.android.ui;

import android.content.Context;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.CommentTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.comments.CommentsActivity;
import org.wordpress.android.ui.media.MediaBrowserActivity;
import org.wordpress.android.ui.notifications.NotificationsActivity;
import org.wordpress.android.ui.posts.PagesActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.reader.ReaderPostListActivity;
import org.wordpress.android.ui.stats.StatsActivity;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;

import java.util.ArrayList;

/*
 * used by DrawerAdapter to maintain a list of items in the drawer
 */

public class DrawerItems {

    private final ArrayList<DrawerItem> mItems = new ArrayList<DrawerItem>();

    public DrawerItems() {
        super();
        refresh();
    }

    private void addIfVisible(DrawerItemId itemId) {
        DrawerItem item = new DrawerItem(itemId);
        if (item.isVisible()) {
            mItems.add(item);
        }
    }

    /*
     * reset the item list and add all items that should be visible
     */
    void refresh() {
        mItems.clear();

        addIfVisible(DrawerItemId.READER);
        addIfVisible(DrawerItemId.NOTIFICATIONS);
        addIfVisible(DrawerItemId.POSTS);
        addIfVisible(DrawerItemId.MEDIA);
        addIfVisible(DrawerItemId.PAGES);
        addIfVisible(DrawerItemId.COMMENTS);
        addIfVisible(DrawerItemId.THEMES);
        addIfVisible(DrawerItemId.STATS);
        addIfVisible(DrawerItemId.QUICK_PHOTO);
        addIfVisible(DrawerItemId.VIEW_SITE);
    }

    int size() {
        return mItems.size();
    }

    DrawerItem get(int position) {
        if (position < 0 || position >= mItems.size()) {
            return null;
        }
        return mItems.get(position);
    }

    boolean hasSelectedItem(Context context) {
        for (DrawerItem item: mItems) {
            if (item.isSelected(context)) {
                return true;
            }
        }
        return false;
    }

    /*
     *
     */
    enum DrawerItemId {
        READER,
        NOTIFICATIONS,
        POSTS,
        MEDIA,
        PAGES,
        COMMENTS,
        THEMES,
        STATS,
        VIEW_SITE,
        QUICK_PHOTO;

        ActivityId toActivityId() {
            switch (this) {
                case READER:
                    return ActivityId.READER;
                case NOTIFICATIONS:
                    return ActivityId.NOTIFICATIONS;
                case POSTS:
                    return ActivityId.POSTS;
                case MEDIA:
                    return ActivityId.MEDIA;
                case PAGES:
                    return ActivityId.PAGES;
                case COMMENTS:
                    return ActivityId.COMMENTS;
                case THEMES:
                    return ActivityId.THEMES;
                case STATS:
                    return ActivityId.STATS;
                case VIEW_SITE:
                    return ActivityId.VIEW_SITE;
                case QUICK_PHOTO:
                    return ActivityId.UNKNOWN;
                default :
                    return ActivityId.UNKNOWN;
            }
        }
    }

    /*
     *
     */
    static class DrawerItem {
        private final DrawerItemId mItemId;

        DrawerItem(DrawerItemId itemId) {
            mItemId = itemId;
        }

        DrawerItemId getDrawerItemId() {
            return mItemId;
        }

        int getTitleResId() {
            switch (mItemId) {
                case READER:
                    return R.string.reader;
                case NOTIFICATIONS:
                    return R.string.notifications;
                case POSTS:
                    return R.string.posts;
                case MEDIA:
                    return R.string.media;
                case PAGES:
                    return R.string.pages;
                case COMMENTS:
                    return R.string.tab_comments;
                case THEMES:
                    return R.string.themes;
                case STATS:
                    return R.string.tab_stats;
                case VIEW_SITE:
                    return R.string.view_site;
                case QUICK_PHOTO:
                    return R.string.quick_photo;
                default :
                    return 0;
            }
        }

        int getIconResId() {
            switch (mItemId) {
                case READER:
                    return R.drawable.noticon_reader_alt_black;
                case NOTIFICATIONS:
                    return R.drawable.noticon_notification_black;
                case POSTS:
                    return R.drawable.dashicon_admin_post_black;
                case MEDIA:
                    return R.drawable.dashicon_admin_media_black;
                case PAGES:
                    return R.drawable.dashicon_admin_page_black;
                case COMMENTS:
                    return R.drawable.dashicon_admin_comments_black;
                case THEMES:
                    return R.drawable.dashboard_icon_themes;
                case STATS:
                    return R.drawable.noticon_milestone_black;
                case VIEW_SITE:
                    return R.drawable.noticon_show_black;
                case QUICK_PHOTO:
                    return R.drawable.dashicon_camera_black;
                default :
                    return 0;
            }
        }

        int getBadgeCount() {
            if (mItemId == DrawerItemId.COMMENTS) {
                return CommentTable.getUnmoderatedCommentCount(WordPress.getCurrentLocalTableBlogId());
            } else {
                return 0;
            }
        }

        boolean isSelected(Context context) {
            switch (mItemId) {
                case READER:
                    return context instanceof ReaderPostListActivity;
                case NOTIFICATIONS:
                    return context instanceof NotificationsActivity;
                case POSTS:
                    return (context instanceof PostsActivity) && !(context instanceof PagesActivity);
                case MEDIA:
                    return context instanceof MediaBrowserActivity;
                case PAGES:
                    return context instanceof PagesActivity;
                case COMMENTS:
                    return context instanceof CommentsActivity;
                case THEMES:
                    return context instanceof ThemeBrowserActivity;
                case STATS:
                    return context instanceof StatsActivity;
                case VIEW_SITE:
                    return context instanceof ViewSiteActivity;
                case QUICK_PHOTO:
                    return false;
                default :
                    return false;
            }
        }

        boolean isVisible() {
            switch (mItemId) {
                case READER:
                    return WordPress.hasDotComToken(WordPress.getContext());
                case NOTIFICATIONS:
                    return WordPress.hasDotComToken(WordPress.getContext());
                case POSTS:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case MEDIA:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case PAGES:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case COMMENTS:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case THEMES:
                    Blog blog = WordPress.getCurrentBlog();
                    return (blog != null && blog.isAdmin() && blog.isDotcomFlag());
                case STATS:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case VIEW_SITE:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                case QUICK_PHOTO:
                    return WordPress.wpDB.getNumVisibleAccounts() != 0;
                default :
                    return false;
            }
        }

        /*
         * returns true if the item should have a divider beneath it
         */
        boolean hasDivider() {
            return (mItemId == DrawerItemId.NOTIFICATIONS);
        }
    }
}
