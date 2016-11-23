package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.services.ReaderPostService;
import org.wordpress.android.ui.reader.services.ReaderPostService.UpdateAction;
import org.wordpress.android.util.NetworkUtils;

/**
 * marker view between posts indicating a gap in time between them that can be filled in - designed
 * for use inside ReaderPostAdapter
 */
public class ReaderGapMarkerView extends RelativeLayout {
    private TextView mText;
    private ProgressBar mProgress;
    private ReaderTag mCurrentTag;

    public ReaderGapMarkerView(Context context) {
        super(context);
        initView(context);
    }

    public ReaderGapMarkerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderGapMarkerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_gap_marker_view, this);
        mText = (TextView) view.findViewById(R.id.text_gap_marker);
        mProgress = (ProgressBar) view.findViewById(R.id.progress_gap_marker);

        mText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fillTheGap();
            }
        });
    }

    public void setCurrentTag(ReaderTag tag) {
        mCurrentTag = tag;
        hideProgress();
    }

    private void fillTheGap() {
        if (mCurrentTag == null
                || !NetworkUtils.checkConnection(getContext())) {
            return;
        }

        // start service to fill the gap - EventBus will notify the owning fragment of new posts,
        // and will take care of hiding this view
        ReaderPostService.startServiceForTag(getContext(), mCurrentTag, UpdateAction.REQUEST_OLDER_THAN_GAP);
        showProgress();
    }

    private void showProgress() {
        mText.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mText.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);
    }
}
