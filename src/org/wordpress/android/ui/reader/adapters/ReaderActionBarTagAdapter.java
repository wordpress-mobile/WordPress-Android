package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;

/**
 * populates ActionBar dropdown with reader tags
 */
public class ReaderActionBarTagAdapter extends BaseAdapter {
    private ReaderTagList mTags = new ReaderTagList();
    private final LayoutInflater mInflater;
    private final ReaderActions.DataLoadedListener mDataListener;
    private final int mPaddingForStaticDrawer;
    private final boolean mIsStaticMenuDrawer;

    public ReaderActionBarTagAdapter(Context context, boolean isStaticMenuDrawer, ReaderActions.DataLoadedListener dataListener) {
        mDataListener = dataListener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // if the menu drawer is static (which it is for landscape tablets) add extra left padding
        // so that the list of tags appears flush left with the fragment (ie: to the right of the
        // menu drawer). without this, the tag list will be all the way to the left where it's not
        // obvious to the user that it changes the reader's view
        mIsStaticMenuDrawer = isStaticMenuDrawer;
        mPaddingForStaticDrawer = context.getResources().getDimensionPixelOffset(R.dimen.menu_drawer_width)
                                - DisplayUtils.dpToPx(context, 32); // 32dp is the size of the ActionBar home icon

        refreshTags();
    }

    public int getIndexOfTagName(String tagName) {
        if (tagName==null)
            return -1;
        for (int i=0; i < mTags.size(); i++) {
            if (tagName.equalsIgnoreCase(mTags.get(i).getTagName()))
                return i;
        }
        return -1;
    }

    public void refreshTags() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "reader tag adapter > Load tags task already running");
        }

        new LoadTagsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void reloadTags() {
        mTags.clear();
        refreshTags();
    }

    @Override
    public int getCount() {
        return (mTags !=null ? mTags.size() : 0);
    }

    @Override
    public Object getItem(int index) {
        return mTags.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ReaderTag tag = mTags.get(position);
        view = mInflater.inflate(R.layout.reader_actionbar_item, null);
        TextView txtName = (TextView) view.findViewById(R.id.text);
        txtName.setText(tag.getCapitalizedTagName());
        if (mIsStaticMenuDrawer)
            txtName.setPadding(mPaddingForStaticDrawer, 0, 0, 0);
        return view;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        ReaderTag tag = mTags.get(position);
        view = mInflater.inflate(R.layout.reader_actionbar_dropdown_item, null);
        TextView txtName = (TextView) view.findViewById(R.id.text);
        txtName.setText(tag.getCapitalizedTagName());
        if (mIsStaticMenuDrawer)
            txtName.setGravity(Gravity.RIGHT);
        return view;
    }

    private boolean mIsTaskRunning = false;
    private class LoadTagsTask extends AsyncTask<Void, Void, Boolean> {
        private final ReaderTagList tmpTags = new ReaderTagList();
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            tmpTags.addAll(ReaderTagTable.getDefaultTags());
            tmpTags.addAll(ReaderTagTable.getSubscribedTags());
            if (mTags.isSameList(tmpTags))
                return false;
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTags = (ReaderTagList) tmpTags.clone();
                notifyDataSetChanged();
                if (mDataListener != null)
                    mDataListener.onDataLoaded(mTags.isEmpty());
            }
            mIsTaskRunning = false;
        }
    }
}
