package org.wordpress.android.ui.accounts;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectBlogsDialog {
    private List mUsersBlogsList;
    private SelectBlogsDialog.Listener mListener;
    private Context mContext;

    public interface Listener {
        void onSuccess(SparseBooleanArray selectedBlogs);
        void onCancel();
    }

    public SelectBlogsDialog(Listener listener, Context context) {
        mListener = listener;
        mContext = context;
    }

    public void showBlogSelectionDialog(List usersBlogsList) {
        mUsersBlogsList = usersBlogsList;
        if (mUsersBlogsList == null) {
            mUsersBlogsList = new ArrayList();
        }
        if (mUsersBlogsList.size() == 0) {
            mListener.onSuccess(null);
            return;
        }
        if (mUsersBlogsList.size() == 1) {
            // Just add the one blog and finish up
            SparseBooleanArray oneBlogArray = new SparseBooleanArray();
            oneBlogArray.put(0, true);
            mListener.onSuccess(oneBlogArray);
            return;
        }
        if (mUsersBlogsList != null && mUsersBlogsList.size() != 0) {
            SparseBooleanArray allBlogs = new SparseBooleanArray();
            for (int i = 0; i < mUsersBlogsList.size(); i++) {
                allBlogs.put(i, true);
            }
            LayoutInflater inflater =  (LayoutInflater) mContext.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            final ListView listView = (ListView) inflater.inflate(R.layout.select_blogs_list,
                    null);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
            listView.setItemsCanFocus(false);
            final UsersBlogsArrayAdapter adapter = new UsersBlogsArrayAdapter(mContext,
                    R.layout.blogs_row, mUsersBlogsList);
            listView.setAdapter(adapter);
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(mContext);
            dialogBuilder.setTitle(R.string.select_blogs);
            dialogBuilder.setView(listView);
            dialogBuilder.setNegativeButton(R.string.add_selected,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SparseBooleanArray selectedBlogs = listView.
                                    getCheckedItemPositions();
                            mListener.onSuccess(selectedBlogs);
                        }
                    });
            dialogBuilder.setPositiveButton(R.string.add_all,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            SparseBooleanArray allBlogs = new SparseBooleanArray();
                            for (int i = 0; i < adapter.getCount(); i++) {
                                allBlogs.put(i, true);
                            }
                            mListener.onSuccess(allBlogs);
                        }
                    });
            dialogBuilder.setOnKeyListener(new ProgressDialog.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    mListener.onCancel();
                    return false;
                }
            });
            dialogBuilder.setCancelable(true);
            AlertDialog ad = dialogBuilder.create();
            ad.setInverseBackgroundForced(true);
            ad.show();

            final Button addSelected = ad.getButton(AlertDialog.BUTTON_NEGATIVE);
            addSelected.setEnabled(false);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
                    SparseBooleanArray selectedItems = listView.getCheckedItemPositions();
                    boolean isChecked = false;
                    for (int i = 0; i < selectedItems.size(); i++) {
                        if (selectedItems.get(selectedItems.keyAt(i)) == true) {
                            isChecked = true;
                        }
                    }
                    if (!isChecked) {
                        addSelected.setEnabled(false);
                    } else {
                        addSelected.setEnabled(true);
                    }
                }
            });
        }
    }

    private class UsersBlogsArrayAdapter extends ArrayAdapter {
        public UsersBlogsArrayAdapter(Context context, int resource, List<Object> list) {
            super(context, resource, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.blogs_row, parent, false);
            }

            Map<String, Object> blogMap = (HashMap<String, Object>) mUsersBlogsList.get(position);
            if (blogMap != null) {
                CheckedTextView blogTitleView = (CheckedTextView)
                        convertView.findViewById(R.id.blog_title);
                String blogTitle = blogMap.get("blogName").toString();
                if (blogTitle != null && blogTitle.trim().length() > 0) {
                    blogTitleView.setText(StringUtils.unescapeHTML(blogTitle));
                } else {
                    blogTitleView.setText(blogMap.get("url").toString());
                }
            }
            return convertView;
        }
    }
}
