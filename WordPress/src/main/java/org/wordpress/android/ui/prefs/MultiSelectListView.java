package org.wordpress.android.ui.prefs;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wordpress.android.R;

/**
 * ListView that supports multiple item selection and provides select all and delete buttons.
 */
public class MultiSelectListView extends ListView
        implements AdapterView.OnItemLongClickListener,
        AdapterView.OnItemClickListener,
        ActionMode.Callback {

    public interface OnEnterMultiSelect {
        void onEnterMultiSelect();
    }

    public interface OnExitMultiSelect {
        void onExitMultiSelect();
    }

    public interface OnDeleteRequested {
        /**
         * @return
         * true to exit Action Mode
         */
        boolean onDeleteRequested();
    }

    private OnEnterMultiSelect mEnterListener;
    private OnExitMultiSelect mExitListener;
    private OnDeleteRequested mDeleteListener;
    private ActionMode mActionMode;

    public MultiSelectListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnItemClickListener(this);
        setOnItemLongClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode == null) return;

        if (getCheckedItemCount() <= 0) {
            mActionMode.finish();
        } else {
            int color = isItemChecked(position) ? R.color.white : R.color.transparent;
            getChildAt(position - getFirstVisiblePosition()).setBackgroundColor(getResources().getColor(color));
            mActionMode.invalidate();
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        if (mActionMode != null) return false;

        setItemChecked(position, true);
        getChildAt(position - getFirstVisiblePosition()).setBackgroundColor(getResources().getColor(R.color.white));
        mActionMode = startActionMode(this);
        if (mEnterListener != null) mEnterListener.onEnterMultiSelect();

        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.list_editor, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        mode.setTitle(String.valueOf(getCheckedItemCount()));
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_delete:
                if (mDeleteListener == null || mDeleteListener.onDeleteRequested()) {
                    mActionMode.finish();
                }

                return true;
            case R.id.menu_select_all:
                for (int i = 0; i < getCount(); i++) {
                    setItemChecked(i, true);
                }

                mActionMode.invalidate();

                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        for (int i = 0; i < getCount(); i++) {
            setItemChecked(i, false);
        }

        clearChoices();
        mActionMode = null;
        if (mExitListener != null) mExitListener.onExitMultiSelect();
    }

    public void setEnterMultiSelectListener(OnEnterMultiSelect listener) {
        mEnterListener = listener;
    }

    public void setExitMultiSelectListener(OnExitMultiSelect listener) {
        mExitListener = listener;
    }

    public void setDeleteRequestListener(OnDeleteRequested listener) {
        mDeleteListener = listener;
    }
}
