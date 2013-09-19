package org.wordpress.android.ui.posts;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import org.wordpress.android.R;
import org.wordpress.android.models.CategoryNode;

import java.util.List;


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
        ImageView levelIndicatorView = (ImageView) rowView.findViewById(R.id.categoryRowLevelIndicator);
        textView.setText(Html.fromHtml(getItem(position).getName()));
        int level = getItem(position).getLevel();
        if (level == 1) { // hide ImageView
            levelIndicatorView.setVisibility(View.GONE);
        } else {
            ViewGroup.LayoutParams params = levelIndicatorView.getLayoutParams();
            params.width = (params.width / 2) * level;
            levelIndicatorView.setLayoutParams(params);
        }
        return rowView;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(mResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        ImageView levelIndicatorView = (ImageView) rowView.findViewById(R.id.categoryRowLevelIndicator);
        textView.setText(Html.fromHtml(getItem(position).getName()));
        levelIndicatorView.setVisibility(View.GONE);
        return rowView;
    }
}