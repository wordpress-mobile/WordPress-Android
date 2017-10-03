package org.wordpress.android.ui.posts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.util.DisplayUtils;

import java.util.List;

import static org.wordpress.android.WordPress.getContext;

public class ParentCategorySpinnerAdapter extends BaseAdapter implements SpinnerAdapter {
    int mResourceId;
    List<CategoryNode> mObjects;
    Context mContext;

    public int getCount() {
        return mObjects.size();
    }

    public CategoryNode getItem(int position) {
        return mObjects.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    public ParentCategorySpinnerAdapter(Context context, int resource, List<CategoryNode> objects) {
        super();
        mContext = context;
        mObjects = objects;
        mResourceId = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(mResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        textView.setText(StringEscapeUtils.unescapeHtml4(getItem(position).getName()));
        CategoryNode node = getItem(position);
        if (node != null) {
            textView.setText(StringEscapeUtils.unescapeHtml4(node.getName()));
            textView.setPadding(DisplayUtils.dpToPx(getContext(), 16) * (node.getLevel() + 1), 0,
                    DisplayUtils.dpToPx(getContext(), 16), 0);
        }
        return rowView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(mResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        textView.setText(StringEscapeUtils.unescapeHtml4(getItem(position).getName()));
        return rowView;
    }
}
