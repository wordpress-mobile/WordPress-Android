package org.wordpress.android.ui.mysite;

import android.content.Intent;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.SitePickerActivity;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPTextView;

public class MySiteFragment extends Fragment {

    private WPNetworkImageView mBlavatarImageView;
    private WPTextView mBlogTitleTextView;
    private WPTextView mBlogSubtitleTextView;
    private int mBlavatarSz;

    private Blog mBlog;

    public static MySiteFragment newInstance() {
        MySiteFragment fragment = new MySiteFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    public void setBlog(Blog blog) {
        mBlog = blog;

        refreshBlogDetails();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // we want to open the last used blog by default
        int lastBlogId = WordPress.wpDB.getLastBlogId();
        mBlog = WordPress.getBlog(lastBlogId);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_my_site, container, false);

        mBlavatarSz = getResources().getDimensionPixelSize(R.dimen.blavatar_sz_small);
        mBlavatarImageView = (WPNetworkImageView) rootView.findViewById(R.id.my_site_blavatar);
        mBlogTitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_title_label);
        mBlogSubtitleTextView = (WPTextView) rootView.findViewById(R.id.my_site_subtitle_label);

        Button switchSiteButton = (Button) rootView.findViewById(R.id.switch_site);
        switchSiteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), SitePickerActivity.class);
                getActivity().startActivityForResult(intent, RequestCodes.SITE_PICKER);
            }
        });

        refreshBlogDetails();

        return rootView;
    }

    private void refreshBlogDetails() {
        if (!isAdded() || mBlog == null) {
            return;
        }

        mBlavatarImageView.setImageUrl(GravatarUtils.blavatarFromUrl(mBlog.getUrl(), mBlavatarSz), WPNetworkImageView.ImageType.BLAVATAR);
        mBlogTitleTextView.setText(mBlog.getBlogName());
        mBlogSubtitleTextView.setText(StringUtils.getHost(mBlog.getUrl()));
    }
}
