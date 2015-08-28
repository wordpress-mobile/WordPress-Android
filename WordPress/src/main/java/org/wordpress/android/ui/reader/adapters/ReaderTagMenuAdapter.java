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

public class ReaderTagMenuAdapter extends BaseAdapter {
    private final ReaderTagList mTags = new ReaderTagList();
    private final LayoutInflater mInflater;

    public ReaderTagMenuAdapter(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        loadTags();
    }

    private void loadTags() {
        new LoadTagsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    public View getView(int position, View convertView, ViewGroup parent) {
        final ReaderTag tag = mTags.get(position);
        final TagViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.reader_tag_toolbar_menu_item, parent, false);
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

    private class LoadTagsTask extends AsyncTask<Void, Void, ReaderTagList> {
        @Override
        protected ReaderTagList doInBackground(Void... voids) {
            ReaderTagList tagList = ReaderTagTable.getDefaultTags();
            tagList.addAll(ReaderTagTable.getFollowedTags());
            return tagList;
        }
        @Override
        protected void onPostExecute(ReaderTagList tagList) {
            if (tagList != null && !tagList.isSameList(mTags)) {
                mTags.clear();
                mTags.addAll(tagList);
                notifyDataSetChanged();
            }
        }
    }
}
