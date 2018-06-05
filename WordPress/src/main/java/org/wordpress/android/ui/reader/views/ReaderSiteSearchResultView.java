package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ReaderFeedModel;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPNetworkImageView;
import org.wordpress.android.widgets.WPNetworkImageView.ImageType;

/**
 * single feed search result
 */
public class ReaderSiteSearchResultView extends LinearLayout {
    private ReaderFollowButton mFollowButton;
    private ReaderFeedModel mSite;

    public ReaderSiteSearchResultView(Context context) {
        this(context, null);
    }

    public ReaderSiteSearchResultView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReaderSiteSearchResultView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_site_search_result, this);
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(params);
        mFollowButton = view.findViewById(R.id.follow_button);
        mFollowButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                toggleFollowStatus();
            }
        });
    }

    public void setSite(@NonNull ReaderFeedModel site) {
        mSite = site;

        TextView txtTitle = findViewById(R.id.text_title);
        TextView txtUrl = findViewById(R.id.text_url);
        WPNetworkImageView imgBlavatar = findViewById(R.id.image_blavatar);

        txtTitle.setText(site.getTitle());
        txtUrl.setText(UrlUtils.getHost(site.getUrl()));
        imgBlavatar.setImageUrl(site.getIconUrl(), ImageType.BLAVATAR);
    }

    private void toggleFollowStatus() {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !mSite.isFollowing();
        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }
                mFollowButton.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_follow_blog
                            : R.string.reader_toast_err_unfollow_blog;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                    mSite.setFollowing(!isAskingToFollow);
                }
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result = ReaderBlogActions.followFeedById(mSite.getFeedId(), isAskingToFollow, listener);
        if (result) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
            mSite.setFollowing(isAskingToFollow);
        }
    }
}
