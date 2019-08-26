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
}
