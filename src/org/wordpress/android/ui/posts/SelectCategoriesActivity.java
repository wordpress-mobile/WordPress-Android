package org.wordpress.android.ui.posts;

import java.util.*;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.wordpress.android.models.CategoryNode;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.StringUtils;

public class SelectCategoriesActivity extends SherlockListActivity {
    private XMLRPCClient client;
    String finalResult = "";
    ProgressDialog pd;
    public String categoryErrorMsg = "";
    private final Handler mHandler = new Handler();
    private Blog blog;
    private ListView mListView;
    private int mListViewScrollStateOffset, mListViewScrollStateIndex;
    private HashSet<String> mSelectedCategories;
    private CategoryNode mCategories;
    private ArrayList<CategoryNode> mCategoryLevels;
    private Map<String, Integer> mCategoryNames = new HashMap<String, Integer>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.select_categories);
        setTitle(getResources().getString(R.string.select_categories));

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        mListView = getListView();
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setItemsCanFocus(false);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                if (getCheckedItemCount(mListView) > 1) {
                    boolean uncategorizedNeedToBeSelected = false;
                    for (int i = 0; i < mCategoryLevels.size(); i++) {
                        if ( mCategoryLevels.get(i).getName().equalsIgnoreCase("uncategorized") ) {
                            mListView.setItemChecked(i, uncategorizedNeedToBeSelected);
                        }
                    }
                }
            }
        });

        mSelectedCategories = new HashSet<String>();

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            int blogId = extras.getInt("id");
            try {
                blog = new Blog(blogId);
            } catch (Exception e) {
                Toast.makeText(this, getResources().getText(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
                finish();
            }
            if (extras.containsKey("categories"))
                mSelectedCategories = (HashSet<String>) extras.getSerializable("categories");
        }
        if (mSelectedCategories == null) {
            mSelectedCategories = new HashSet<String>();
        }

        populateOrFetchCategories();
    }

    private void populateCategoryList() {
        mCategoryLevels = CategoryNode.getSortedListOfCategoriesFromRoot(mCategories);
        for (int i = 0; i < mCategoryLevels.size(); i++) {
            mCategoryNames.put( StringUtils.unescapeHTML( mCategoryLevels.get(i).getName() ), i);
        }

        CategoryArrayAdapter categoryAdapter = new CategoryArrayAdapter(this, R.layout.categories_row, mCategoryLevels);
        this.setListAdapter(categoryAdapter);
        if (mSelectedCategories != null) {
            ListView lv = getListView();
            for (String selectedCategory : mSelectedCategories) {
                if (mCategoryNames.keySet().contains(selectedCategory)) {
                    lv.setItemChecked(mCategoryNames.get(selectedCategory), true);
                }
            }
        }
        restoreScrollOffset();
    }


    private void populateOrFetchCategories() {
        mCategories = CategoryNode.createCategoryTreeFromDB(blog.getId());

        if (mCategories.getChildren().size() > 0) {
            populateCategoryList();
        } else {
            refreshCategories();
        }
    }

    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            if (finalResult.equals("addCategory_success")) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                populateOrFetchCategories();

                Toast.makeText(SelectCategoriesActivity.this, getResources().getText(R.string.adding_cat_success), Toast.LENGTH_SHORT).show();
            }
            if (finalResult.equals("addCategory_failed")) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SelectCategoriesActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.adding_cat_failed));
                dialogBuilder.setMessage(getResources().getText(R.string.adding_cat_failed_check));
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                if (!isFinishing())
                    dialogBuilder.create().show();
            } else if (finalResult.equals("gotCategories")) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }
                populateOrFetchCategories();
                Toast.makeText(SelectCategoriesActivity.this, getResources().getText(R.string.categories_refreshed), Toast.LENGTH_SHORT).show();
            } else if (finalResult.equals("FAIL")) {
                if (pd.isShowing()) {
                    pd.dismiss();
                }

                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(SelectCategoriesActivity.this);
                dialogBuilder.setTitle(getResources().getText(R.string.category_refresh_error));
                dialogBuilder.setMessage(categoryErrorMsg);
                dialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Just close the window.

                    }
                });
                dialogBuilder.setCancelable(true);
                if (!isFinishing())
                    dialogBuilder.create().show();
            }
        }
    };

    /**
     * Gets the categories via a xmlrpc call
     * @return result message
     */
    public String fetchCategories() {
        String returnMessage;
        Object result[] = null;
        Object[] params = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), };
        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

        boolean success = false;

        try {
            result = (Object[]) client.call("wp.getCategories", params);
            success = true;
        } catch (XMLRPCException e) {
            e.printStackTrace();
        }

        if (success) {
            // wipe out the categories table
            WordPress.wpDB.clearCategories(blog.getId());

            for (Object aResult : result) {
                Map<?, ?> curHash = (Map<?, ?>) aResult;
                String categoryName = curHash.get("categoryName").toString();
                String categoryID = curHash.get("categoryId").toString();
                String categoryParentID = curHash.get("parentId").toString();
                int convertedCategoryID = Integer.parseInt(categoryID);
                int convertedCategoryParentID = Integer.parseInt(categoryParentID);
                WordPress.wpDB.insertCategory(blog.getId(), convertedCategoryID, convertedCategoryParentID, categoryName);
            }
            returnMessage = "gotCategories";
        } else {
            returnMessage = "FAIL";
        }
        return returnMessage;
    }

    /**
     * function addCategory
     *
     * @param String
     *            category_name
     * @return
     * @description Adds a new category
     */
    public String addCategory(String category_name, String category_slug, String category_desc, int parent_id) {
        // Return string
        String returnString = "";

        // Save selected categories
        updateSelectedCategoryList();
        saveScrollOffset();

        // Store the parameters for wp.addCategory
        Map<String, Object> struct = new HashMap<String, Object>();
        struct.put("name", category_name);
        struct.put("slug", category_slug);
        struct.put("description", category_desc);
        struct.put("parent_id", parent_id);

        client = new XMLRPCClient(blog.getUrl(), blog.getHttpuser(), blog.getHttppassword());

        Object[] params = { blog.getBlogId(), blog.getUsername(), blog.getPassword(), struct };

        Object result = null;
        try {
            result = client.call("wp.newCategory", params);
        } catch (XMLRPCException e) {
            e.printStackTrace();
        }

        if (result == null) {
            returnString = "addCategory_failed";
        } else {
            // Category successfully created. "result" is the ID of the new category
            // Initialize the category database
            // Convert "result" (= category_id) from type Object to int
            int category_id = Integer.parseInt(result.toString());
            // Insert the new category into database
            WordPress.wpDB.insertCategory(blog.getId(), category_id, parent_id, category_name);
            returnString = "addCategory_success";
            // auto select new category
            mSelectedCategories.add(category_name);
        }

        return returnString;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {

            final Bundle extras = data.getExtras();

            switch (requestCode) {
            case 0: // Add category
                // Does the user want to continue, or did he press "dismiss"?
                if (extras.getString("continue").equals("TRUE")) {
                    // Get name, slug and desc from Intent
                    final String category_name = extras.getString("category_name");
                    final String category_slug = extras.getString("category_slug");
                    final String category_desc = extras.getString("category_desc");
                    final int parent_id = extras.getInt("parent_id");

                    // Check if the category name already exists
                    if (!mCategoryNames.keySet().contains(category_name)) {
                        pd = ProgressDialog.show(SelectCategoriesActivity.this,
                                getResources().getText(R.string.cat_adding_category),
                                getResources().getText(R.string.cat_attempt_add_category), true, true);
                        Thread th = new Thread() {
                            public void run() {
                                finalResult = addCategory(category_name, category_slug, category_desc, parent_id);
                                mHandler.post(mUpdateResults);
                            }
                        };
                        th.start();
                    }
                    break;
                }
            }// end null check
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.categories, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_refresh) {
            refreshCategories();
            return true;
        } else if (itemId == R.id.menu_new_category) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", blog.getId());
            Intent i = new Intent(SelectCategoriesActivity.this, AddCategoryActivity.class);
            i.putExtras(bundle);
            startActivityForResult(i, 0);
            return true;
        } else if (itemId == android.R.id.home) {
            saveAndFinish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void refreshCategories() {
        saveScrollOffset();
        updateSelectedCategoryList();
        pd = ProgressDialog.show(SelectCategoriesActivity.this, getResources().getText(R.string.refreshing_categories),
                getResources().getText(R.string.attempting_categories_refresh), true, true);
        Thread th = new Thread() {
            public void run() {
                finalResult = fetchCategories();
                mHandler.post(mUpdateResults);
            }
        };
        th.start();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // ignore orientation change
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        saveAndFinish();
        super.onBackPressed();
    }

    private void saveScrollOffset() {
        mListViewScrollStateIndex = mListView.getFirstVisiblePosition();
        View view = mListView.getChildAt(0);
        mListViewScrollStateOffset = 0;
        if (view != null) {
            mListViewScrollStateOffset = view.getTop();
        }
    }

    private void restoreScrollOffset() {
        mListView.setSelectionFromTop(mListViewScrollStateIndex, mListViewScrollStateOffset);
    }

    private void updateSelectedCategoryList() {
        SparseBooleanArray selectedItems = mListView.getCheckedItemPositions();
        for (int i = 0; i < selectedItems.size(); i++) {
            String currentName = StringUtils.unescapeHTML(mCategoryLevels.get(selectedItems.keyAt(i)).getName());
            if (selectedItems.get(selectedItems.keyAt(i))) {
                mSelectedCategories.add(currentName);
            } else {
                mSelectedCategories.remove(currentName);
            }
        }
    }

    private void saveAndFinish() {
        Bundle bundle = new Bundle();
        updateSelectedCategoryList();
        bundle.putSerializable("selectedCategories", new ArrayList<String>(mSelectedCategories));
        Intent mIntent = new Intent();
        mIntent.putExtras(bundle);
        setResult(RESULT_OK, mIntent);
        finish();
    }

    private int getCheckedItemCount(ListView listView)
    {
        if (Build.VERSION.SDK_INT >= 11) return listView.getCheckedItemCount();
        else
        {
            int count = 0;
            for (int i = listView.getCount() - 1; i >= 0; i--)
                if (listView.isItemChecked(i)) count++;
            return count;
        }
    }
}
