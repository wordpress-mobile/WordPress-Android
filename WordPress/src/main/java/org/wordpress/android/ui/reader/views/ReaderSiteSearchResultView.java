package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.fluxc.model.ReaderSiteModel;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderBlogActions;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

/**
 * single feed search result
 */
public class ReaderSiteSearchResultView extends LinearLayout {
    public interface OnSiteFollowedListener {
        void onSiteFollowed(@NonNull ReaderSiteModel site);
        void onSiteUnFollowed(@NonNull ReaderSiteModel site);
    }

    private ReaderFollowButton mFollowButton;
    private ReaderSiteModel mSite;
    private OnSiteFollowedListener mFollowListener;

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

    public void setSite(@NonNull ReaderSiteModel site, @NonNull OnSiteFollowedListener followListener) {
        mSite = site;
        mFollowListener = followListener;

        TextView txtTitle = findViewById(R.id.text_title);
        TextView txtUrl = findViewById(R.id.text_url);
        ImageView imgBlavatar = findViewById(R.id.image_blavatar);

        if (!TextUtils.isEmpty(site.getTitle())) {
            txtTitle.setText(site.getTitle());
        } else {
            txtTitle.setText(R.string.untitled_in_parentheses);
        }
        txtUrl.setText(UrlUtils.getHost(site.getUrl()));
        ImageManager.getInstance().load(imgBlavatar, ImageType.BLAVATAR, StringUtils.notNullStr(site.getIconUrl()));
        mFollowButton.setIsFollowed(site.isFollowing());
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
            if (isAskingToFollow) {
                mFollowListener.onSiteFollowed(mSite);
            } else {
                mFollowListener.onSiteUnFollowed(mSite);
            }
        }
    }
}
