package org.wordpress.android.ui.reader.adapters;

import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.ReaderInterfaces;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

/**
 * populates spinner with reader tags
 */
public class ReaderTagSpinnerAdapter extends BaseAdapter {
    private ReaderTagList mTags = new ReaderTagList();
    private final LayoutInflater mInflater;
    private final ReaderInterfaces.DataLoadedListener mDataListener;

    public ReaderTagSpinnerAdapter(Context context) {
        this(context, null);
    }
    public ReaderTagSpinnerAdapter(Context context, ReaderInterfaces.DataLoadedListener dataListener) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDataListener = dataListener;
        refreshTags();
    }

    public int getIndexOfTag(ReaderTag tag) {
        if (tag == null) {
            return -1;
        }
        for (int i = 0; i < mTags.size(); i++) {
            if (ReaderTag.isSameTag(tag, mTags.get(i))) {
                return i;
            }
        }
        return -1;
    }

    public void refreshTags() {
        if (mIsTaskRunning) {
            AppLog.w(T.READER, "reader tag adapter > Load tags task already running");
        } else {
            new LoadTagsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public void reloadTags() {
        mTags.clear();
        refreshTags();
    }

    @Override
    public int getCount() {
        return (mTags !=null ? mTags.size() : 0);
    }

    private boolean isValidPosition(int position) {
        return (position >= 0 && position < getCount());
    }

    @Override
    public Object getItem(int index) {
        if (isValidPosition(index)) {
            return mTags.get(index);
        } else {
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderTag tag = mTags.get(position);
        final TagViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_spinner_item, parent, false);
            holder = new TagViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (TagViewHolder) convertView.getTag();
        }

        holder.textView.setText(tag.getCapitalizedTagName());
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        final ReaderTag tag = mTags.get(position);
        final TagViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_spinner_dropdown_item, parent, false);
            holder = new TagViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (TagViewHolder) convertView.getTag();
        }

        holder.textView.setText(tag.getCapitalizedTagName());
        return convertView;
    }

    private class TagViewHolder {
        private final TextView textView;
        TagViewHolder(View view) {
            textView = (TextView) view.findViewById(R.id.text);
        }
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
            tmpTags.addAll(ReaderTagTable.getFollowedTags());
            return !mTags.isSameList(tmpTags);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTags = (ReaderTagList) tmpTags.clone();
                notifyDataSetChanged();
                if (mDataListener != null) {
                    mDataListener.onDataLoaded(mTags.isEmpty());
                }
            }
            mIsTaskRunning = false;
        }
    }
}
