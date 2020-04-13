package org.wordpress.android.ui.posts;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;

import static org.wordpress.android.ui.posts.PostSettingsTagsActivity.KEY_TAGS;

public class PostSettingsTagsFragment extends TagsFragment {
    public static final String TAG = "post_settings_tags_fragment_tag";

    public static PostSettingsTagsFragment newInstance(@NonNull SiteModel site, @Nullable String tags) {
        Bundle args = new Bundle();
        args.putSerializable(WordPress.SITE, site);
        args.putString(KEY_TAGS, tags);

        PostSettingsTagsFragment fragment = new PostSettingsTagsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override protected int getContentLayout() {
        return R.layout.fragment_post_settings_tags;
    }
}

