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
        clickOn(R.id.quick_action_posts_button);
        return this;
    }
<<<<<<< HEAD
=======

>>>>>>> 08af53e32d62b1f12e4ddc0cda6f29025481acd7
}
