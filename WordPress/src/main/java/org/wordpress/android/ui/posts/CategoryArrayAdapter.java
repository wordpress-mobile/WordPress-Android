package org.wordpress.android.ui.posts;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
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
    private int mResourceId;

    CategoryArrayAdapter(Context context, int resource, List<CategoryNode> objects) {
        super(context, resource, objects);
        mResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = convertView;
        if (rowView == null) {
            rowView = inflater.inflate(mResourceId, parent, false);
            ViewHolder viewHolder = new ViewHolder(rowView);
            rowView.setTag(viewHolder);
        }

        ViewHolder viewHolder = (ViewHolder) rowView.getTag();
        CategoryNode node = getItem(position);
        if (node != null) {
            viewHolder.mCategoryRowText.setText(StringEscapeUtils.unescapeHtml4(node.getName()));
            ViewCompat.setPaddingRelative(viewHolder.mCategoryRowText,
                                          DisplayUtils.dpToPx(getContext(), 16) * node.getLevel(), 0,
                                          DisplayUtils.dpToPx(getContext(), 16), 0);
        }
        return rowView;
    }

    private static class ViewHolder {
        private final TextView mCategoryRowText;

        private ViewHolder(View view) {
            this.mCategoryRowText = view.findViewById(R.id.categoryRowText);
        }
    }
}
