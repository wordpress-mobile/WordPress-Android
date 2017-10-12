package org.wordpress.android.ui.posts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.models.CategoryNode;
import org.wordpress.android.util.DisplayUtils;

import java.util.List;

public class CategoryArrayAdapter extends ArrayAdapter<CategoryNode> {
    int mResourceId;

    public CategoryArrayAdapter(Context context, int resource, List<CategoryNode> objects) {
        super(context, resource, objects);
        mResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(mResourceId, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        CategoryNode node = getItem(position);
        if (node != null) {
            textView.setText(StringEscapeUtils.unescapeHtml4(node.getName()));
            textView.setPadding(DisplayUtils.dpToPx(getContext(), 16) * node.getLevel(), 0,
                    DisplayUtils.dpToPx(getContext(), 16), 0);
        }
        return rowView;
    }
}
