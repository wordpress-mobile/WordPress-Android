package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;

/**
 * marker view between posts indicating a gap in time between them
 */
public class ReaderGapMarkerView extends RelativeLayout {
    private TextView mText;
    private ImageView mImage;
    private ProgressBar mProgress;

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
        mImage = (ImageView) view.findViewById(R.id.image_gap_marker);
        mProgress = (ProgressBar) view.findViewById(R.id.progress_gap_marker);

        mText.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showProgress();
            }
        });
    }

    private void showProgress() {
        mText.setVisibility(View.INVISIBLE);
        mImage.setVisibility(View.INVISIBLE);
        mProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        mText.setVisibility(View.VISIBLE);
        mImage.setVisibility(View.VISIBLE);
        mProgress.setVisibility(View.GONE);
    }
}
