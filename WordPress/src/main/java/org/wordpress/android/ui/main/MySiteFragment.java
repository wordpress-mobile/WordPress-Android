package org.wordpress.android.ui.main;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.MediaAddFragment;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.ui.themes.ThemeBrowserActivity;
import org.wordpress.android.util.CoreEvents;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.ServiceUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

import de.greenrobot.event.EventBus;

public class MySiteFragment extends Fragment
        implements WPMainActivity.OnScrollToTopListener {
    public static final String ADD_MEDIA_FRAGMENT_TAG = "add-media-fragment";

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private LinearLayout mLookAndFeelHeader;
    private RelativeLayout mThemesContainer;
    private View mFabView;
    int mScreenWidth;

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
    public void onResume() {
        super.onResume();
        if (ServiceUtils.isServiceRunning(getActivity(), StatsService.class)) {
            getActivity().stopService(new Intent(getActivity(), StatsService.class));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_my_site, container, false);

        FragmentManager fm = getFragmentManager();
        fm.beginTransaction().add(new MediaAddFragment(), ADD_MEDIA_FRAGMENT_TAG).commit();

        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);
        mBlavatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_subtitle_label);
        mLookAndFeelHeader = (LinearLayout) rootView.findViewById(R.id.my_site_look_and_feel_header);
        mThemesContainer = (RelativeLayout) rootView.findViewById(R.id.row_themes);
        mFabView = rootView.findViewById(R.id.fab_button);

        mFabView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.addNewBlogPostOrPage(getActivity(), mBlog, false);
            }
        });

        rootView.findViewById(R.id.switch_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int localBlogId = (mBlog != null ? mBlog.getLocalTableBlogId() : 0);
                ActivityLauncher.showSitePickerForResult(getActivity(), localBlogId);
            }
        });

        rootView.findViewById(R.id.row_view_site).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentSite(getActivity());
            }
        });

        rootView.findViewById(R.id.row_stats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // if the blog is empty, fail silently
                if (mBlog != null) {
                    ActivityLauncher.viewBlogStats(getActivity(), mBlog.getLocalTableBlogId());
                }
            }
        });

        rootView.findViewById(R.id.row_blog_posts).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPosts(getActivity());
            }
        });

        rootView.findViewById(R.id.row_media).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogMedia(getActivity());
            }
        });

        rootView.findViewById(R.id.row_pages).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogPages(getActivity());
            }
        });

        rootView.findViewById(R.id.row_comments).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogComments(getActivity());
            }
        });

        mThemesContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewCurrentBlogThemes(getActivity());
            }
        });

        rootView.findViewById(R.id.row_settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogSettingsForResult(getActivity(), mBlog);
            }
        });

        rootView.findViewById(R.id.row_admin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActivityLauncher.viewBlogAdmin(getActivity(), mBlog);
            }
        });

        refreshBlogDetails();

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        resetFabAnimationValues();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RequestCodes.PICTURE_LIBRARY:
                FragmentManager fm = getFragmentManager();
                Fragment addFragment = fm.findFragmentByTag(ADD_MEDIA_FRAGMENT_TAG);
                if (addFragment != null) {
                    addFragment.onActivityResult(requestCode, resultCode, data);
                }
                break;
            default:
                break;
        }
    }

    private void refreshBlogDetails() {
        if (!isAdded() || mBlog == null) {
            return;
        }

        int themesVisibility = ThemeBrowserActivity.isAccessible() ? View.VISIBLE : View.GONE;
        mLookAndFeelHeader.setVisibility(themesVisibility);
        mThemesContainer.setVisibility(themesVisibility);

        mBlavatarImageView.setImageUrl(GravatarUtils.blavatarFromUrl(mBlog.getUrl(), mBlavatarSz), WPNetworkImageView.ImageType.BLAVATAR);

        String blogName = StringUtils.unescapeHTML(mBlog.getBlogName());
        String hostName = StringUtils.getHost(mBlog.getUrl());
        String blogTitle = TextUtils.isEmpty(blogName) ? hostName : blogName;

        mBlogTitleTextView.setText(blogTitle);
        mBlogSubtitleTextView.setText(hostName);
    }

    @Override
    public void onScrollToTop() {
        if (isAdded()) {
            ScrollView scrollView = (ScrollView) getView().findViewById(R.id.scroll_view);
            scrollView.smoothScrollTo(0, 0);
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    private void resetFabAnimationValues() {
        mScreenWidth = DisplayUtils.getDisplayPixelWidth(getActivity());
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(CoreEvents.MainViewPagerScrolling event) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) mFabView.getLayoutParams();
        int height = mFabView.getHeight();
        int targetYTranslation = layoutParams.bottomMargin + height;
        int pageOffset = ((WPMainActivity) getActivity()).getSelectedPageOnResume();
        int rightScreenBorder = mScreenWidth - pageOffset * mScreenWidth;
        // 0.2 means 20% of the screen is hidden, 1.0 means the view is completly switched
        float relativeOffset = 1 - (rightScreenBorder - event.mXOffset) / (float)  mScreenWidth;
        mFabView.setTranslationY(targetYTranslation * relativeOffset);
    }
}
