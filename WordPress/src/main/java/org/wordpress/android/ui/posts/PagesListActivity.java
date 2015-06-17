package org.wordpress.android.ui.posts;

import org.wordpress.android.ui.ActivityId;

public class PagesListActivity extends PostsListActivity {
    // Exists to distinguish pages from posts in menu drawer

    @Override
    protected void bumpActivityAnalytics() {
        ActivityId.trackLastActivity(ActivityId.PAGES);
    }
}
