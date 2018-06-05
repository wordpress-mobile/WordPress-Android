package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderBlogTable;
import org.wordpress.android.fluxc.model.ReaderFeedModel;
import org.wordpress.android.ui.reader.ReaderInterfaces.OnFollowListener;
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
    private ReaderFeedModel mFeed;
    private OnFollowListener mFollowListener;

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
        View view = inflate(context, R.layout.reader_site_header_view, this);
        mFollowButton = view.findViewById(R.id.follow_button);
    }

    public void setOnFollowListener(OnFollowListener listener) {
        mFollowListener = listener;
    }

    public void setFeed(@NonNull ReaderFeedModel feed) {
        mFeed = feed;

        TextView txtTitle = findViewById(R.id.text_title);
        TextView txtUrl = findViewById(R.id.text_url);
        WPNetworkImageView imgBlavatar = findViewById(R.id.image_blavatar);

        txtTitle.setText(feed.getTitle());
        txtUrl.setText(UrlUtils.getHost(feed.getUrl()));
        imgBlavatar.setImageUrl(feed.getIconUrl(), ImageType.BLAVATAR);
    }

    private void toggleFollowStatus(final View followButton) {
        if (!NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !ReaderBlogTable.isFollowedFeed(mFeed.getFeedId());
        if (mFollowListener != null) {
            if (isAskingToFollow) {
                mFollowListener.onFollowTapped(followButton, mFeed.getTitle(), mFeed.getFeedId());
            } else {
                mFollowListener.onFollowingTapped();
            }
        }

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
                }
            }
        };

        // disable follow button until API call returns
        mFollowButton.setEnabled(false);

        boolean result = ReaderBlogActions.followFeedById(mFeed.getFeedId(), isAskingToFollow, listener);
        if (result) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
