package org.wordpress.android.ui.posts;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wordpress.android.R;
import org.wordpress.android.models.Category;
import org.wordpress.android.util.EscapeUtils;

import java.util.List;

public class CategoryArrayAdapter extends ArrayAdapter<CategoryArrayAdapter.CategoryLevel> {
    // Intermediate structure to populate the ListView
    public static class CategoryLevel {
        private Category category;
        private int level;

        public CategoryLevel(Category category, int level) {
            this.category = category;
            this.level = level;
        }

        public Category getCategory() {
            return category;
        }

        public void setCategory(Category category) {
            this.category = category;
        }

        public int getLevel() {
            return level;
        }

        public void setLevel(int level) {
            this.level = level;
        }
    }

    public CategoryArrayAdapter(Context context, int resource, List<CategoryArrayAdapter.CategoryLevel> objects) {
        super(context, resource, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.categories_row, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.categoryRowText);
        ImageView levelIndicatorView = (ImageView) rowView.findViewById(R.id.categoryRowLevelIndicator);
        textView.setText(EscapeUtils.escapeHtml(getItem(position).getCategory().getName()));
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

}
