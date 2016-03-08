package org.wordpress.android.ui.reader.views;

import android.content.Context;
import android.support.v7.widget.ListPopupWindow;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.adapters.ReaderTagMenuAdapter;
import org.wordpress.android.ui.reader.utils.ReaderUtils;

/**
 * pseudo-toolbar at top of ReaderPostAdapter which enables user to change tags
 */
public class ReaderTagToolbar extends LinearLayout {

    private ReaderTag mCurrentTag;

    public interface OnTagChangedListener {
        void onTagChanged(ReaderTag tag);
    }

    private TextView mTextTagName;
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
        mTextTagName = (TextView) view.findViewById(R.id.text_tag);
        View btnEditTags = view.findViewById(R.id.btn_edit_tags);

        if (!isInEditMode()) {
            mTextTagName.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    showTagPopupMenu(v);
                }
            });

            if (ReaderUtils.isLoggedOutReader()) {
                btnEditTags.setVisibility(View.GONE);
            } else {
                btnEditTags.setVisibility(View.VISIBLE);
                btnEditTags.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ReaderActivityLauncher.showReaderSubs(v.getContext());
                    }
                });

                ReaderUtils.setBackgroundToRoundRipple(btnEditTags);
            }
        }
    }

    public void setOnTagChangedListener(OnTagChangedListener listener) {
        mOnTagChangedListener = listener;
    }

    public void setCurrentTag(ReaderTag tag) {
        mCurrentTag = tag;
        mTextTagName.setText(tag != null ? tag.getCapitalizedTagName() : "");
    }

    /*
     * user tapped the tag name, show a popup menu of tags to choose from
     */
    private void showTagPopupMenu(View view) {
        Context context = view.getContext();
        final ListPopupWindow listPopup = new ListPopupWindow(context);
        listPopup.setAnchorView(view);

        listPopup.setWidth(context.getResources().getDimensionPixelSize(R.dimen.menu_item_width));
        listPopup.setModal(true);
        listPopup.setAdapter(new ReaderTagMenuAdapter(context, mCurrentTag));
        listPopup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                listPopup.dismiss();
                Object object = parent.getItemAtPosition(position);
                if (object instanceof ReaderTag) {
                    ReaderTag tag = (ReaderTag) object;
                    if (mOnTagChangedListener != null) {
                        mOnTagChangedListener.onTagChanged(tag);
                    }
                }
            }
        });
        listPopup.show();
    }
}
