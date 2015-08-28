package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Spinner;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.ui.reader.adapters.ReaderTagSpinnerAdapter;

/**
 * pseudo-toolbar at top of ReaderPostAdapter - contains spinner enabling user to change tags
 */
public class ReaderTagToolbar extends LinearLayout {

    public interface OnTagChangedListener {
        void onTagChanged(ReaderTag tag);
    }

    private Spinner mSpinner;
    private ReaderTagSpinnerAdapter mSpinnerAdapter;
    private View mBtnEditTags;

    private ReaderTag mCurrentTag;
    private OnTagChangedListener mOnTagChangedListener;

    public ReaderTagToolbar(Context context) {
        super(context);
        initView(context);
    }

    public ReaderTagToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public ReaderTagToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context);
    }

    private void initView(Context context) {
        View view = inflate(context, R.layout.reader_tag_toolbar, this);
        mSpinner = (Spinner) view.findViewById(R.id.spinner);
        mBtnEditTags = view.findViewById(R.id.btn_edit_tags);

        if (!isInEditMode()) {
            mSpinner.setAdapter(getSpinnerAdapter());
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    ReaderTag tag = (ReaderTag) getSpinnerAdapter().getItem(position);
                    setCurrentTag(tag);
                    if (mOnTagChangedListener != null) {
                        mOnTagChangedListener.onTagChanged(tag);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nop
                }
            });

            mBtnEditTags.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ReaderActivityLauncher.showReaderSubs(v.getContext());
                }
            });
        }
    }

    public void setOnTagChangedListener(OnTagChangedListener listener) {
        mOnTagChangedListener = listener;
    }

    private ReaderTagSpinnerAdapter getSpinnerAdapter() {
        if (mSpinnerAdapter == null) {
            ReaderInterfaces.DataLoadedListener dataListener = new ReaderInterfaces.DataLoadedListener() {
                @Override
                public void onDataLoaded(boolean isEmpty) {
                    if (isValid()) {
                        selectCurrentTag();
                    }
                }
            };
            mSpinnerAdapter = new ReaderTagSpinnerAdapter(getContext(), dataListener);
        }

        return mSpinnerAdapter;
    }

    private boolean hasSpinnerAdapter() {
        return (mSpinnerAdapter != null);
    }

    private boolean isValid() {
        return getContext() != null;
    }

    public ReaderTag getCurrentTag() {
        return mCurrentTag;
    }

    public void setCurrentTag(ReaderTag tag) {
        if (tag == null || isCurrentTag(tag)) return;

        mCurrentTag = tag;
        selectCurrentTag();
    }

    public boolean hasCurrentTag() {
        return mCurrentTag != null;
    }

    public boolean isCurrentTag(ReaderTag tag) {
        return ReaderTag.isSameTag(tag, mCurrentTag);
    }

    /*
     * make sure the current tag is the one selected in the spinner
     */
    private void selectCurrentTag() {
        if (mSpinner == null
                || !hasSpinnerAdapter()
                || !hasCurrentTag()) {
            return;
        }

        int position = getSpinnerAdapter().getIndexOfTag(getCurrentTag());
        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }
}
