package org.wordpress.android.ui.reader_native.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.ReaderTopicTable;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderTopic.ReaderTopicType;
import org.wordpress.android.models.ReaderTopicList;
import org.wordpress.android.ui.reader_native.actions.ReaderActions;
import org.wordpress.android.ui.reader_native.actions.ReaderTopicActions;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SysUtils;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderTopicAdapter extends BaseAdapter {
    public interface TopicActionListener {
        public void onTopicAction(ReaderTopicActions.TopicAction action, String topicName);
    }

    private LayoutInflater mInflater;
    private ReaderTopicList mTopics = new ReaderTopicList();
    private TopicActionListener mTopicListener;
    private ReaderTopic.ReaderTopicType mTopicType;
    private ReaderActions.DataLoadedListener mDataLoadadListener;
    private Drawable mDrawableAdd;
    private Drawable mDrawableRemove;

    public ReaderTopicAdapter(Context context, TopicActionListener topicListener) {
        super();

        mInflater = LayoutInflater.from(context);
        mTopicListener = topicListener;
        mDrawableAdd = context.getResources().getDrawable(R.drawable.ic_add_topic_dark);
        mDrawableRemove = context.getResources().getDrawable(R.drawable.ic_remove_topic);
    }

    @SuppressLint("NewApi")
    public void refreshTopics(ReaderActions.DataLoadedListener dataListener) {
        if (mIsTaskRunning)
            ReaderLog.w("topic task is already running");
        mDataLoadadListener = dataListener;
        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadTopicsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadTopicsTask().execute();
        }
    }

    public void refreshTopics() {
        refreshTopics(null);
    }

    public ReaderTopic.ReaderTopicType getTopicType() {
        return mTopicType;
    }

    public void setTopicType(ReaderTopicType topicType) {
        mTopicType = (topicType!=null ? topicType : ReaderTopicType.DEFAULT);
        refreshTopics();
    }

    public int indexOfTopicName(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return -1;
        return mTopics.indexOfTopic(topicName);
    }

    /*
     * called when user deletes a topic - by this time the topic has already been removed from the db
     */
    /*public boolean removeTopic(ListView listView, String topicName) {
        if (listView==null || topicName==null)
            return false;

        final int position = mTopics.indexOfTopic(topicName);
        if (position==-1)
            return false;

        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) { }
            @Override
            public void onAnimationEnd(Animation animation) {
                mTopics.remove(position);
                notifyDataSetChanged();
            }
            @Override
            public void onAnimationRepeat(Animation animation) { }
        };
        ReaderAniUtils.removeListItem(listView, position, listener, android.R.anim.fade_out);
        return true;
    }*/

    @Override
    public int getCount() {
        return mTopics.size();
    }

    @Override
    public Object getItem(int position) {
        return mTopics.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderTopic topic = (ReaderTopic) getItem(position);
        TopicViewHolder holder;
        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.listitem_reader_topic, parent, false);
            holder = new TopicViewHolder();
            holder.txtTopic = (TextView) convertView.findViewById(R.id.text_topic);
            holder.btnAddRemove = (ImageButton) convertView.findViewById(R.id.btn_add_remove);
            convertView.setTag(holder);
        } else {
            holder = (TopicViewHolder) convertView.getTag();
        }

        holder.txtTopic.setText(topic.getCapitalizedTopicName());

        switch (topic.topicType) {
            case SUBSCRIBED:
                // only subscribed topics can be deleted
                holder.btnAddRemove.setImageDrawable(mDrawableRemove);
                holder.btnAddRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // tell activity that user wishes to delete this topic
                        if (mTopicListener!=null)
                            mTopicListener.onTopicAction(ReaderTopicActions.TopicAction.DELETE, topic.getTopicName());
                    }
                });
                holder.btnAddRemove.setVisibility(View.VISIBLE);
                break;

            case RECOMMENDED:
                // only recommended topics can be added
                holder.btnAddRemove.setImageDrawable(mDrawableAdd);
                holder.btnAddRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // tell activity that user wishes to add this topic
                        if (mTopicListener!=null)
                            mTopicListener.onTopicAction(ReaderTopicActions.TopicAction.ADD, topic.getTopicName());
                    }
                });
                holder.btnAddRemove.setVisibility(View.VISIBLE);
                break;

            default :
                holder.btnAddRemove.setVisibility(View.GONE);
                break;

        }

        return convertView;
    }

    private static class TopicViewHolder {
        private TextView txtTopic;
        private ImageButton btnAddRemove;
    }

    /*
     * AsyncTask to load topics
     */
    private boolean mIsTaskRunning = false;
    private class LoadTopicsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderTopicList tmpTopics;
        @Override
        protected void onPreExecute() {
            mIsTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            switch (mTopicType) {
                case RECOMMENDED:
                    tmpTopics = ReaderTopicTable.getRecommendedTopics(true);
                    break;
                case SUBSCRIBED:
                    tmpTopics = ReaderTopicTable.getSubscribedTopics();
                    break;
                default :
                    tmpTopics = ReaderTopicTable.getDefaultTopics();
                    break;
            }

            if (tmpTopics==null)
                return false;

            if (mTopics.isSameList(tmpTopics))
                return false;

            return (tmpTopics!=null);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTopics = (ReaderTopicList)(tmpTopics.clone());
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
            if (mDataLoadadListener!=null)
                mDataLoadadListener.onDataLoaded(isEmpty());
        }
    }


}
