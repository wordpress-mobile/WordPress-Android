package org.wordpress.android.ui.suggestion.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagSuggestionAdapter extends BaseAdapter implements Filterable {
    private final LayoutInflater mInflater;
    private Filter mTagFilter;
    private List<Tag> mTagList;
    private List<Tag> mOrigTagList;

    public TagSuggestionAdapter(Context context) {
        mInflater = LayoutInflater.from(context);
    }

    public void setTagList(List<Tag> tagList) {
        mOrigTagList = tagList;
    }

    @Override
    public int getCount() {
        if (mTagList == null) {
            return 0;
        }
        return mTagList.size();
    }

    @Override
    public Tag getItem(int position) {
        if (mTagList == null) {
            return null;
        }
        return mTagList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final TagViewHolder holder;

        if (convertView == null || convertView.getTag() == null) {
            convertView = mInflater.inflate(R.layout.tag_list_row, parent, false);
            holder = new TagViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (TagViewHolder) convertView.getTag();
        }

        Tag tag = getItem(position);

        if (tag != null) {
            holder.txtTag.setText(tag.getTag());
        }

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mTagFilter == null) {
            mTagFilter = new TagFilter();
        }

        return mTagFilter;
    }

    private class TagViewHolder {
        private final TextView txtTag;

        TagViewHolder(View row) {
            txtTag = (TextView) row.findViewById(R.id.tag_list_row_tag_label);
        }
    }

    private class TagFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            if (mOrigTagList == null) {
                results.values = null;
                results.count = 0;
            }
            else if (constraint == null || constraint.length() == 0) {
                results.values = mOrigTagList;
                results.count = mOrigTagList.size();
            }
            else {
                List<Tag> nTagList = new ArrayList<Tag>();

                for (Tag tag : mOrigTagList) {
                    String lowerCaseConstraint = constraint.toString().toLowerCase();
                    if (tag.getTag().toLowerCase().startsWith(lowerCaseConstraint)
                            || tag.getTag().toLowerCase().contains(" " + lowerCaseConstraint))
                        nTagList.add(tag);
                }

                results.values = nTagList;
                results.count = nTagList.size();
            }
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            if (results.count == 0)
                notifyDataSetInvalidated();
            else {
                mTagList = (List<Tag>) results.values;
                notifyDataSetChanged();
            }
        }

        @Override
        public CharSequence convertResultToString (Object resultValue) {
            Tag tag = (Tag) resultValue;
            return tag.getTag();
        }
    }
}
