package org.wordpress.android.ui.posts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.models.CategoryNode;

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
            int verticalPadding = rowView.getResources().getDimensionPixelOffset(R.dimen.margin_large);
            int horizontalPadding = rowView.getResources().getDimensionPixelOffset(R.dimen.margin_extra_large);

            viewHolder.mCategoryRowText.setText(StringEscapeUtils.unescapeHtml4(node.getName()));
            ViewCompat.setPaddingRelative(viewHolder.mCategoryRowText,
                    horizontalPadding * node.getLevel(),
                    verticalPadding,
                    horizontalPadding,
                    verticalPadding);
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
