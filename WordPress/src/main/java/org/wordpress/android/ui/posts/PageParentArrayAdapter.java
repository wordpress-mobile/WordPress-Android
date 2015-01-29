package org.wordpress.android.ui.posts;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.PageNode;

import java.util.List;

public class PageParentArrayAdapter extends ArrayAdapter<PageNode> {
    private final int mResourceId;

    public PageParentArrayAdapter(Context context, int resource, List<PageNode> objects) {
        super(context, resource, objects);
        mResourceId = resource;
    }

    static class ViewHolder {
        TextView title;
        ImageView levelIndicator;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(mResourceId, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.title = (TextView) convertView.findViewById(R.id.pagesRowText);
            viewHolder.levelIndicator = (ImageView) convertView.findViewById(R.id.pageRowLevelIndicator);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.title.setText(Html.fromHtml(getItem(position).getName()));

        // Handle indicator visibility and indentation
        int level = getItem(position).getLevel();
        ViewGroup.LayoutParams params = viewHolder.levelIndicator.getLayoutParams();
        int baseWidth = (int) getContext().getResources().getDimension(R.dimen.category_row_height);
        if (level == 1) {
            params.width = baseWidth;
            viewHolder.levelIndicator.setLayoutParams(params);
            viewHolder.levelIndicator.setVisibility(View.GONE);
        } else {
            params.width = (baseWidth / 2) * level;
            viewHolder.levelIndicator.setLayoutParams(params);
            viewHolder.levelIndicator.setVisibility(View.VISIBLE);
        }

        return convertView;
    }
}