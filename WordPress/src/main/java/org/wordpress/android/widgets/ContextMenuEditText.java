package org.wordpress.android.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;

/**
 * WPEditText which notifies when text is cut, copied, or pasted.
 */
public class ContextMenuEditText extends AppCompatEditText {
    public interface OnContextMenuListener {
        void onCut();

        void onCopy();

        void onPaste();
    }

    private OnContextMenuListener mOnContextMenuListener;

    /**
     * Set a listener to interface with activity or fragment.
     *
     * @param listener object listening for cut, copy, and paste events
     */
    public void setOnContextMenuListener(OnContextMenuListener listener) {
        mOnContextMenuListener = listener;
    }

    public ContextMenuEditText(Context context) {
        super(context);
    }

    public ContextMenuEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ContextMenuEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * ContextMenu used to cut, copy, or paste which overwrites the consuming method.
     */
    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = super.onTextContextMenuItem(id);

        switch (id) {
            case android.R.id.cut:
                onCut();
                break;
            case android.R.id.copy:
                onCopy();
                break;
            case android.R.id.paste:
                onPaste();
                break;
        }

        return consumed;
    }

    /**
     * Text cut from WPEditText.
     */
    public void onCut() {
        if (mOnContextMenuListener != null) {
            mOnContextMenuListener.onCut();
        }
    }

    /**
     * Text copied from WPEditText.
     */
    public void onCopy() {
        if (mOnContextMenuListener != null) {
            mOnContextMenuListener.onCopy();
        }
    }

    /**
     * Text pasted into WPEditText.
     */
    public void onPaste() {
        if (mOnContextMenuListener != null) {
            mOnContextMenuListener.onPaste();
        }
    }
}
