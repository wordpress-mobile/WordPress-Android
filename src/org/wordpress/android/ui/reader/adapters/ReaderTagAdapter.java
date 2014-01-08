package org.wordpress.android.ui.reader.adapters;

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
import org.wordpress.android.datasets.ReaderTagTable;
import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.actions.ReaderActions;
import org.wordpress.android.ui.reader.actions.ReaderTagActions;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.SysUtils;

/**
 * Created by nbradbury on 6/27/13.
 */
public class ReaderTagAdapter extends BaseAdapter {
    public interface TopicActionListener {
        public void onTopicAction(ReaderTagActions.TagAction action, String topicName);
    }

    private LayoutInflater mInflater;
    private ReaderTagList mTopics = new ReaderTagList();
    private TopicActionListener mTopicListener;
    private ReaderTag.ReaderTagType mTopicType;
    private ReaderActions.DataLoadedListener mDataLoadadListener;
    private Drawable mDrawableAdd;
    private Drawable mDrawableRemove;

    public ReaderTagAdapter(Context context, TopicActionListener topicListener) {
        super();

        mInflater = LayoutInflater.from(context);
        mTopicListener = topicListener;
        mDrawableAdd = context.getResources().getDrawable(R.drawable.ic_content_new);
        mDrawableRemove = context.getResources().getDrawable(R.drawable.ic_content_remove);
    }

    @SuppressLint("NewApi")
    public void refreshTopics(ReaderActions.DataLoadedListener dataListener) {
        if (mIsTaskRunning)
            AppLog.w(T.READER, "topic task is already running");
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

    public ReaderTag.ReaderTagType getTopicType() {
        return mTopicType;
    }

    public void setTopicType(ReaderTag.ReaderTagType topicType) {
        mTopicType = (topicType!=null ? topicType : ReaderTag.ReaderTagType.DEFAULT);
        refreshTopics();
    }

    public int indexOfTopicName(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return -1;
        return mTopics.indexOfTag(topicName);
    }

    /*
     * called when user deletes a topic - by this time the topic has already been removed from the db
     */
    /*public boolean removeTopic(ListView listView, String topicName) {
        if (listView==null || topicName==null)
            return false;

        final int position = mTopics.indexOfTag(topicName);
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
        AniUtils.removeListItem(listView, position, listener, android.R.anim.fade_out);
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
        final ReaderTag topic = (ReaderTag) getItem(position);
        TopicViewHolder holder;
        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_tag, parent, false);
            holder = new TopicViewHolder();
            holder.txtTopic = (TextView) convertView.findViewById(R.id.text_topic);
            holder.btnAddRemove = (ImageButton) convertView.findViewById(R.id.btn_add_remove);
            convertView.setTag(holder);
        } else {
            holder = (TopicViewHolder) convertView.getTag();
        }

        holder.txtTopic.setText(topic.getCapitalizedTagName());

        switch (topic.tagType) {
            case SUBSCRIBED:
                // only subscribed topics can be deleted
                holder.btnAddRemove.setImageDrawable(mDrawableRemove);
                holder.btnAddRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // tell activity that user wishes to delete this topic
                        if (mTopicListener!=null)
                            mTopicListener.onTopicAction(ReaderTagActions.TagAction.DELETE, topic.getTagName());
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
                            mTopicListener.onTopicAction(ReaderTagActions.TagAction.ADD, topic.getTagName());
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
        ReaderTagList tmpTopics;
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
                    tmpTopics = ReaderTagTable.getRecommendedTags(true);
                    break;
                case SUBSCRIBED:
                    tmpTopics = ReaderTagTable.getSubscribedTags();
                    break;
                default :
                    tmpTopics = ReaderTagTable.getDefaultTags();
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
                mTopics = (ReaderTagList)(tmpTopics.clone());
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
            if (mDataLoadadListener!=null)
                mDataLoadadListener.onDataLoaded(isEmpty());
        }
    }


}
