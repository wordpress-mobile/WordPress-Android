package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.ui.reader.utils.ReaderUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;

/**
 * topmost view in post adapter when showing tag preview - displays tag name and follow button
 */
public class ReaderTagInfoView extends LinearLayout {

    private ReaderFollowButton mFollowButton;
    private ReaderTag mCurrentTag;

    public ReaderTagInfoView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderTagInfoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderTagInfoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_tag_info_view, this);
        mFollowButton = (ReaderFollowButton) view.findViewById(R.id.follow_button);
    }

    public void setCurrentTag(final ReaderTag tag) {
        if (tag == null) return;

        mCurrentTag = tag;

        TextView txtTagName = (TextView) findViewById(R.id.text_tag);
        txtTagName.setText(ReaderUtils.makeHashTag(tag.getTagSlug()));

        if (ReaderUtils.isLoggedOutReader()) {
            mFollowButton.setVisibility(View.GONE);
        } else {
            mFollowButton.setVisibility(View.VISIBLE);
            mFollowButton.setIsFollowed(ReaderTagTable.isFollowedTagName(tag.getTagSlug()));
            mFollowButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleFollowStatus();
                }
            });
        }
    }

    private void toggleFollowStatus() {
        if (mCurrentTag == null || !NetworkUtils.checkConnection(getContext())) {
            return;
        }

        final boolean isAskingToFollow = !ReaderTagTable.isFollowedTagName(mCurrentTag.getTagSlug());

        ReaderActions.ActionListener listener = new ReaderActions.ActionListener() {
            @Override
            public void onActionResult(boolean succeeded) {
                if (getContext() == null) {
                    return;
                }
                mFollowButton.setEnabled(true);
                if (!succeeded) {
                    int errResId = isAskingToFollow ? R.string.reader_toast_err_add_tag : R.string.reader_toast_err_remove_tag;
                    ToastUtils.showToast(getContext(), errResId);
                    mFollowButton.setIsFollowed(!isAskingToFollow);
                }
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
