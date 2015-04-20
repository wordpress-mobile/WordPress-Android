package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class MySiteFragment extends Fragment
        implements WPMainActivity.OnScrollToTopListener {

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private RelativeLayout mThemesContainer;
    private int mBlavatarSz;

    private Blog mBlog;

    public static MySiteFragment newInstance() {
        return new MySiteFragment();
    }

    public void setBlog(Blog blog) {
        mBlog = blog;

        refreshBlogDetails();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBlog = WordPress.getCurrentBlog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_my_site, container, false);

        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);
        mBlavatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = (LinearLayout) rootView.findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = (RelativeLayout) rootView.findViewById(R.id.my_site_themes_container);

        WPTextView switchSiteTextView = (WPTextView) rootView.findViewById(R.id.switch_site);
        switchSiteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.showSitePickerForResult(getActivity());
            }
        });

        WPTextView viewSiteTextView = (WPTextView) rootView.findViewById(R.id.my_site_view_site_text_view);
        viewSiteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentSite(getActivity());
            }
        });

        WPTextView statsTextView = (WPTextView) rootView.findViewById(R.id.my_site_stats_text_view);
        statsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogStats(getActivity(), mBlog.getLocalTableBlogId());
            }
        });

        WPTextView blogPostsTextView = (WPTextView) rootView.findViewById(R.id.my_site_blog_posts_text_view);
        blogPostsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPosts(getActivity());
            }
        });

        WPTextView mediaTextView = (WPTextView) rootView.findViewById(R.id.my_site_media_text_view);
        mediaTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogMedia(getActivity());
            }
        });

        WPTextView pagesTextView = (WPTextView) rootView.findViewById(R.id.my_site_pages_text_view);
        pagesTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPages(getActivity());
            }
        });

        WPTextView commentsTextView = (WPTextView) rootView.findViewById(R.id.my_site_comments_text_view);
        commentsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogComments(getActivity());
            }
        });

        WPTextView themesTextView = (WPTextView) rootView.findViewById(R.id.my_site_themes_text_view);
        themesTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogThemes(getActivity());
            }
        });

        WPTextView settingsTextView = (WPTextView) rootView.findViewById(R.id.my_site_settings_text_view);
        settingsTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSettings(getActivity(), mBlog);
            }
        });

        WPTextView viewAdminTextView = (WPTextView) rootView.findViewById(R.id.my_site_view_admin_text_view);
        viewAdminTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(getActivity(), mBlog);
            }
        });

        LinearLayout addPostContainer = (LinearLayout) rootView.findViewById(R.id.my_site_posts_add_button_container);
        addPostContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPage(getActivity(), mBlog, false);
            }
        });

        LinearLayout addPageContainer = (LinearLayout) rootView.findViewById(R.id.my_site_pages_add_button_container);
        addPageContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPage(getActivity(), mBlog, true);
            }
        });

        LinearLayout addMediaContainer = (LinearLayout) rootView.findViewById(R.id.my_site_media_add_button_container);
        addMediaContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addMedia(getActivity(), mBlog);
            }
        });

        refreshBlogDetails();

        return rootView;
    }

    private void refreshBlogDetails() {
        if (!isAdded() || mBlog == null) {
            return;
        }

        int themesVisibility = ThemeBrowserActivity.isAccessible() ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        mBlavatarImageView.setImageUrl(GravatarUtils.blavatarFromUrl(mBlog.getUrl(), mBlavatarSz), WPNetworkImageView.ImageType.BLAVATAR);
        mBlogTitleTextView.setText(mBlog.getBlogName());
        mBlogSubtitleTextView.setText(StringUtils.getHost(mBlog.getUrl()));
    }

    @Override
    public void onScrollToTop() {
        if (isAdded()) {
            ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll_view);
            scrollView.smoothScrollTo(0, 0);
        }
    }
}
