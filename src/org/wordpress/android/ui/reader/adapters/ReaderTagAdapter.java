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

public class ReaderTagAdapter extends BaseAdapter {
    public interface TagActionListener {
        public void onTagAction(ReaderTagActions.TagAction action, String tagName);
    }

    private final LayoutInflater mInflater;
    private ReaderTagList mTags = new ReaderTagList();
    private final TagActionListener mTagListener;
    private ReaderTag.ReaderTagType mTagType;
    private ReaderActions.DataLoadedListener mDataLoadadListener;
    private final Drawable mDrawableAdd;
    private final Drawable mDrawableRemove;

    public ReaderTagAdapter(Context context, TagActionListener tagListener) {
        super();

        mInflater = LayoutInflater.from(context);
        mTagListener = tagListener;
        mDrawableAdd = context.getResources().getDrawable(R.drawable.ic_content_new);
        mDrawableRemove = context.getResources().getDrawable(R.drawable.ic_content_remove);
    }

    @SuppressLint("NewApi")
    public void refreshTags(ReaderActions.DataLoadedListener dataListener) {
        if (mIsTaskRunning)
            AppLog.w(T.READER, "tag task is already running");
        mDataLoadadListener = dataListener;
        if (SysUtils.canUseExecuteOnExecutor()) {
            new LoadTagsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new LoadTagsTask().execute();
        }
    }

    public void refreshTags() {
        refreshTags(null);
    }

    public void setTagType(ReaderTag.ReaderTagType tagType) {
        mTagType = (tagType!=null ? tagType : ReaderTag.ReaderTagType.DEFAULT);
        refreshTags();
    }

    public int indexOfTagName(String tagName) {
        if (TextUtils.isEmpty(tagName))
            return -1;
        return mTags.indexOfTag(tagName);
    }

    @Override
    public int getCount() {
        return mTags.size();
    }

    @Override
    public Object getItem(int position) {
        return mTags.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderTag tag = (ReaderTag) getItem(position);
        TagViewHolder holder;
        if (convertView==null) {
            convertView = mInflater.inflate(R.layout.reader_listitem_tag, parent, false);
            holder = new TagViewHolder();
            holder.txtTagName = (TextView) convertView.findViewById(R.id.text_topic);
            holder.btnAddRemove = (ImageButton) convertView.findViewById(R.id.btn_add_remove);
            convertView.setTag(holder);
        } else {
            holder = (TagViewHolder) convertView.getTag();
        }

        holder.txtTagName.setText(tag.getCapitalizedTagName());

        switch (tag.tagType) {
            case SUBSCRIBED:
                // only subscribed tags can be deleted
                holder.btnAddRemove.setImageDrawable(mDrawableRemove);
                holder.btnAddRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // tell activity that user wishes to delete this tag
                        if (mTagListener !=null)
                            mTagListener.onTagAction(ReaderTagActions.TagAction.DELETE, tag.getTagName());
                    }
                });
                holder.btnAddRemove.setVisibility(View.VISIBLE);
                break;

            case RECOMMENDED:
                // only recommended tags can be added
                holder.btnAddRemove.setImageDrawable(mDrawableAdd);
                holder.btnAddRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // tell activity that user wishes to add this tag
                        if (mTagListener !=null)
                            mTagListener.onTagAction(ReaderTagActions.TagAction.ADD, tag.getTagName());
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

    private static class TagViewHolder {
        private TextView txtTagName;
        private ImageButton btnAddRemove;
    }

    /*
     * AsyncTask to load tags
     */
    private boolean mIsTaskRunning = false;
    private class LoadTagsTask extends AsyncTask<Void, Void, Boolean> {
        ReaderTagList tmpTags;
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
            switch (mTagType) {
                case RECOMMENDED:
                    tmpTags = ReaderTagTable.getRecommendedTags(true);
                    break;
                case SUBSCRIBED:
                    tmpTags = ReaderTagTable.getSubscribedTags();
                    break;
                default :
                    tmpTags = ReaderTagTable.getDefaultTags();
                    break;
            }

            if (tmpTags ==null)
                return false;

            if (mTags.isSameList(tmpTags))
                return false;

            return (tmpTags !=null);
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mTags = (ReaderTagList)(tmpTags.clone());
                notifyDataSetChanged();
            }
            mIsTaskRunning = false;
            if (mDataLoadadListener!=null)
                mDataLoadadListener.onDataLoaded(isEmpty());
        }
    }

}
