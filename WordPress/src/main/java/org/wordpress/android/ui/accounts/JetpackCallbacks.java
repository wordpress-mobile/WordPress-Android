package org.wordpress.android.ui.accounts;

import org.wordpress.android.models.Blog;

public interface JetpackCallbacks {
    boolean isJetpackAuth();
    Blog getJetpackBlog();
}