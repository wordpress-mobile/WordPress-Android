package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
public class ReaderTagHeaderView extends RelativeLayout {
    private ReaderFollowButton mFollowButton;
    private ReaderTag mCurrentTag;

    @Inject AccountStore mAccountStore;

    public ReaderTagHeaderView(Context context) {
        this(context, null);
    }

    public ReaderTagHeaderView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReaderTagHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ((WordPress) context.getApplicationContext()).component().inject(this);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_tag_header_view, this);
        mFollowButton = view.findViewById(R.id.follow_button);
    }

    public void setCurrentTag(final ReaderTag tag) {
        if (tag == null) {
            return;
        }

        boolean isTagChanged = !ReaderTag.isSameTag(tag, mCurrentTag);

        if (isTagChanged) {
            mCurrentTag = tag;
        }

        TextView txtTagName = findViewById(R.id.text_tag);
        txtTagName.setText(tag.getLabel());

        if (!mAccountStore.hasAccessToken()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(ReaderTagTable.isFollowedTagName(tag.getTagSlug()));
            mFollowButton.setOnClickListener(v -> toggleFollowStatus());
        }
    }

    private void toggleFollowStatus() {
        if (mCurrentTag == null || !NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(mCurrentTag.getTagSlug());

        ReaderActions.ActionListener listener = succeeded -> {
            if (getContext() == null) {
                return;
            }
            mFollowButton.setEnabled(true);
            if (!succeeded) {
                int errResId = isAskingToFollow ? R.string.reader_toast_err_add_tag
                        : R.string.reader_toast_err_remove_tag;
                ToastUtils.showToast(getContext(), errResId);
                mFollowButton.setIsFollowed(!isAskingToFollow);
            }
        };

        mFollowButton.setEnabled(false);

        boolean success;
        if (isAskingToFollow) {
            success = ReaderTagActions.addTag(mCurrentTag, listener);
        } else {
            success = ReaderTagActions.deleteTag(mCurrentTag, listener);
        }

        if (success) {
            mFollowButton.setIsFollowedAnimated(isAskingToFollow);
        }
    }
}
