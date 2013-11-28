package org.wordpress.android.ui.accounts;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.MapUtils;

import java.util.List;
import java.util.Map;

public class ManageBlogsActivity extends ListActivity {
    List<Map<String, Object>> mAccounts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.blogs_visibility));
        ListView listView = getListView();
        mAccounts = WordPress.wpDB.getAccountsBy(null, new String[] {"isHidden"});
        listView.setAdapter(new BlogsAdapter(this, R.layout.manageblogs_listitem, mAccounts));
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        CheckedTextView checkedView = (CheckedTextView) v;
        checkedView.setChecked(!checkedView.isChecked());
        int blogId = MapUtils.getMapInt(mAccounts.get(position), "id");
        Blog blog = WordPress.getBlog(blogId);
        if (blog == null) {
            Log.e(WordPress.TAG, "Error, blog id not found: " + blogId);
            return ;
        }
        blog.setHidden(!checkedView.isChecked());
        blog.save();
    }

    private class BlogsAdapter extends ArrayAdapter<Map<String, Object>> {
        private int mResource;

        public BlogsAdapter(Context context, int resource, List objects) {
            super(context, resource, objects);
            mResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext().
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(mResource, parent, false);
            CheckedTextView nameView = (CheckedTextView) rowView.findViewById(R.id.blog_name);
            nameView.setText(MapUtils.getMapStr(getItem(position), "blogName"));
            nameView.setChecked(!MapUtils.getMapBool(getItem(position), "isHidden"));
            return rowView;
        }
    }
}