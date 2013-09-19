package org.wordpress.android.ui.reader_native.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderTopicList;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;

/**
 * Created by nbradbury on 7/10/13.
 * populates ActionBar dropdown with reader topics
 */
public class ReaderActionBarTopicAdapter extends BaseAdapter {
    private ReaderTopicList mTopics = new ReaderTopicList();
    private final LayoutInflater mInflater;
    private final ReaderActions.DataLoadedListener mDataListener;

    public ReaderActionBarTopicAdapter(Context context, ReaderActions.DataLoadedListener dataListener) {
        mDataListener = dataListener;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        refreshTopics();
    }

    public int getIndexOfTopicName(String topicName) {
        if (topicName==null)
            return -1;
        for (int i=0; i < mTopics.size(); i++) {
            if (topicName.equalsIgnoreCase(mTopics.get(i).getTopicName()))
                return i;
        }
        return -1;
    }

    @SuppressLint("NewApi")
    public void refreshTopics() {
        if (mIsTaskRunning)
            ReaderLog.w("Load topics task already running");

        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadTopicsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadTopicsTask().execute();
        }
    }

    @Override
    public int getCount() {
        return (mTopics!=null ? mTopics.size() : 0);
    }

    @Override
    public Object getItem(int index) {
        return mTopics.get(index);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {
        ReaderTopic topic = mTopics.get(position);
        view = mInflater.inflate(R.layout.reader_actionbar_item, null);
        TextView txtName = (TextView) view.findViewById(R.id.text);
        txtName.setText(topic.getCapitalizedTopicName());
        return view;
    }

    @Override
    public View getDropDownView(int position, View view, ViewGroup parent) {
        ReaderTopic topic = mTopics.get(position);
        view = mInflater.inflate(R.layout.reader_actionbar_dropdown_item, null);
        TextView txtName = (TextView) view.findViewById(R.id.text);
        txtName.setText(topic.getCapitalizedTopicName());
        return view;
    }

    private boolean mIsTaskRunning = false;
    private class LoadTopicsTask extends AsyncTask<Void, Void, Boolean> {
        private ReaderTopicList tmpTopics = new ReaderTopicList();
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
            tmpTopics.addAll(ReaderTopicTable.getDefaultTopics());
            tmpTopics.addAll(ReaderTopicTable.getSubscribedTopics());
            if (mTopics.isSameList(tmpTopics))
                return false;
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTopics = (ReaderTopicList) tmpTopics.clone();
                notifyDataSetChanged();
                if (mDataListener!=null)
                    mDataListener.onDataLoaded(mTopics.isEmpty());
            }
            mIsTaskRunning = false;
        }
    }
}
