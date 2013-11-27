package org.wordpress.android.ui.posts;

import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.util.WPMobileStatsUtil;

public class PagesActivity extends PostsActivity {
    // Exists to distinguish pages from posts in menu drawer

    @Override
    protected String statEventForViewOpening() {
        return WPMobileStatsUtil.StatsEventPagesOpened;
    }

    @Override
    protected String statEventForViewClosing() {
        return WPMobileStatsUtil.StatsEventPagesClosed;
    }

    @Override
    protected String statEventForNewPost() {
        return WPMobileStatsUtil.StatsEventPagesClickedNewPage;
    }
}
