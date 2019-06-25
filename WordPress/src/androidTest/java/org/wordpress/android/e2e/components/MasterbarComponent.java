package org.wordpress.android.e2e.components;

import org.wordpress.android.R;

import static org.wordpress.android.support.WPSupportUtils.clickOn;

public class MasterbarComponent {
    public MasterbarComponent() {
    }

    public MasterbarComponent goToMySitesTab() {
        clickOn(R.id.nav_sites);
        return this;
    }

    public MasterbarComponent clickBlogPosts() {
        clickOn(R.id.row_blog_posts);
        return this;
    }

    public MasterbarComponent clickSitePages() {
        clickOn(R.id.row_pages);
        return this;
    }

    public MasterbarComponent clickActivity() {
        clickOn(R.id.row_activity_log);
        return this;
    }

}
