package org.wordpress.android.ui.posts;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.format.DateUtils;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnCreateContextMenuListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAlertDialogFragment;

public class PostsListFragment extends ListFragment {
    /** Called when the activity is first created. */
    private ArrayList<String> mPostIDs, mTitles, mDateCreated, mDateCreatedFormatted, mDraftIDs,
                mDraftTitles, mDraftDateCreated, mStatuses, mDraftStatuses, s_mPostIDs, s_mTitles, s_mDateCreatedFormatted, s_mStatuses;
    private int[] mUploaded;
    private int mRowID = 0;
    private long mSelectedID;
    private PostListAdapter mPostListAdapter;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnRefreshListener mOnRefreshListener;
    private OnPostActionListener mOnPostActionListener;
    private PostsActivity mParentActivity;
    private ListView listView;
    public boolean inDrafts = false;
    public List<String> imageUrl = new Vector<String>();
    public String errorMsg = "";
    public int totalDrafts = 0;
    public boolean isPage = false, shouldSelectAfterLoad = false;
    public int numRecords = 20;
    public ViewSwitcher switcher;
    public getRecentPostsTask getPostsTask;
    
    private static final int MENU_GROUP_PAGES = 2, MENU_GROUP_POSTS = 0, MENU_GROUP_DRAFTS = 1;
    private static final int MENU_ITEM_EDIT = 0, MENU_ITEM_DELETE = 1, MENU_ITEM_PREVIEW = 2, MENU_ITEM_SHARE = 3, MENU_ITEM_ADD_COMMENT = 4;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            isPage = extras.getBoolean("viewPages");
        }
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);

        createSwitcher();
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnRefreshListener = (OnRefreshListener) activity;
            mOnPostActionListener = (OnPostActionListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();

        mParentActivity = (PostsActivity) getActivity();

    }

    public void createSwitcher() {
        // create the ViewSwitcher in the current context
        switcher = new ViewSwitcher(getActivity().getApplicationContext());
        Button footer = (Button) View.inflate(getActivity()
                .getApplicationContext(), R.layout.list_footer_btn, null);
        footer.setText(getResources().getText(R.string.load_more) + " "
                + getResources().getText((isPage)? R.string.tab_pages : R.string.tab_posts));

        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {

                if (!WordPress.wpDB.findLocalChanges()) {
                    // first view is showing, show the second progress view
                    switcher.showNext();
                    // get 20 more posts
                    numRecords += 20;
                    refreshPosts(true);
                } else {
                    if (!getActivity().isFinishing()) {
                        FragmentTransaction ft = getFragmentManager()
                                .beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment
                                .newInstance(getString(R.string.remote_changes), getString(R.string.local_changes), true);
                        alert.show(ft, "alert");
                    }
                }
            }
        });

        View progress = View.inflate(getActivity().getApplicationContext(),
                R.layout.list_footer_progress, null);

        switcher.addView(footer);
        switcher.addView(progress);

    }

    public void refreshPosts(final boolean loadMore) {

        if (!loadMore) {
            mOnRefreshListener.onRefresh(true);
            numRecords = 20;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.currentBlog);
        apiArgs.add(isPage);
        apiArgs.add(numRecords);
        apiArgs.add(loadMore);
        getPostsTask = new getRecentPostsTask();
        getPostsTask.execute(apiArgs);
    }

    public Map<String, ?> createItem(String title, String caption) {
        Map<String, String> item = new HashMap<String, String>();
        item.put("title", title);
        item.put("caption", caption);
        return item;
    }

    public boolean loadPosts(boolean loadMore) { // loads posts from the db
        List<Map<String, Object>> loadedPosts;
        try {
            if (isPage) {
                loadedPosts = WordPress.wpDB.loadUploadedPosts(WordPress.currentBlog.getId(), true);
            } else {
                loadedPosts = WordPress.wpDB.loadUploadedPosts(WordPress.currentBlog.getId(),
                        false);
            }
        } catch (Exception e1) {
            return false;
        }

        if (loadedPosts != null) {
            numRecords = loadedPosts.size();
            mTitles = new ArrayList<String>(loadedPosts.size());
            mPostIDs = new ArrayList<String>(loadedPosts.size());
            mDateCreated = new ArrayList<String>(loadedPosts.size());
            mDateCreatedFormatted = new ArrayList<String>(loadedPosts.size());
            mStatuses = new ArrayList<String>(loadedPosts.size());
        } else {
            mTitles = new ArrayList<String>();
            mPostIDs = new ArrayList<String>();
            mDateCreated = new ArrayList<String>();
            mDateCreatedFormatted = new ArrayList<String>();
            mStatuses = new ArrayList<String>();
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataChanged();
            }
        }
        if (loadedPosts != null) {
            Date d = new Date();
            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                mTitles.add(i, StringUtils.unescapeHTML(contentHash.get("title")
                        .toString()));

                mPostIDs.add(i, contentHash.get("id").toString());
                mDateCreated.add(i, contentHash.get("date_created_gmt").toString());

                if (contentHash.get("post_status") != null) {
                    String api_status = contentHash.get("post_status")
                            .toString();
                    if (api_status.equals("publish")) {
                        mStatuses.add(i, getResources()
                                .getText(R.string.published).toString());
                    } else if (api_status.equals("draft")) {
                        mStatuses.add(i, getResources().getText(R.string.draft)
                                .toString());
                    } else if (api_status.equals("pending")) {
                        mStatuses.add(i, getResources().getText(
                                R.string.pending_review).toString());
                    } else if (api_status.equals("private")) {
                        mStatuses.add(i, getResources().getText(
                                R.string.post_private).toString());
                    }

                    if ((Long) contentHash.get("date_created_gmt") > d
                            .getTime() && api_status.equals("publish")) {
                        mStatuses.add(i, getResources()
                                .getText(R.string.scheduled).toString());
                    }
                }

                long localTime = (Long) contentHash.get("date_created_gmt");
                mDateCreatedFormatted.add(i, getFormattedDate(localTime));
            }
        }
        // load drafts
        boolean drafts = loadDrafts();

        if (drafts) {
            mPostIDs = StringUtils.mergeArrayList(mDraftIDs, mPostIDs);
            mTitles = StringUtils.mergeArrayList(mDraftTitles, mTitles);
            mDateCreatedFormatted = StringUtils.mergeArrayList(
                    mDraftDateCreated, mDateCreatedFormatted);
            mStatuses = StringUtils.mergeArrayList(mDraftStatuses, mStatuses);
        } else {
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataChanged();
            }
        }

        if (loadedPosts != null || drafts == true) {
            listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setBackgroundColor(getResources().getColor(R.color.list_row_bg));
            listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
            listView.setDividerHeight(1);
            listView.removeFooterView(switcher);
            if (loadedPosts != null) {
                if (loadedPosts.size() >= 20) {
                    listView.addFooterView(switcher);
                }
            }

            if (loadMore) {
                mPostListAdapter.notifyDataChanged();
            } else {
                
                mPostListAdapter = new PostListAdapter(getActivity().getBaseContext());
                //Calling to initialize the ArrayList
                initArrayList();
                listView.setAdapter(mPostListAdapter);

                listView.setOnItemClickListener(new OnItemClickListener() {

                    public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                        
                        if (position >= mPostIDs.size()) //out of bounds
                            return;

                        if (v == null) //view is gone
                            return;

                        if( !mParentActivity.isRefreshing ) { 
                            mSelectedID = v.getId();
                            Post post = new Post(WordPress.currentBlog
                                    .getId(), mSelectedID, isPage);
                            if (post.getId() >= 0) {
                                WordPress.currentPost = post;
                                mOnPostSelectedListener.onPostSelected(post);
                                mPostListAdapter.notifyDataChanged();
                            } else {
                                if (!getActivity().isFinishing()) {
                                    FragmentTransaction ft = getFragmentManager()
                                            .beginTransaction();
                                    WPAlertDialogFragment alert = WPAlertDialogFragment
                                            .newInstance(getString(R.string.post_not_found));
                                    alert.show(ft, "alert");
                                }
                            }
                        } else {
                            Toast.makeText(mParentActivity, R.string.please_wait_refresh_done, Toast.LENGTH_SHORT).show();
                        }
                        
                    }
                });

                listView.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {

                    public void onCreateContextMenu(ContextMenu menu, View v,
                            ContextMenuInfo menuInfo) {
                        AdapterView.AdapterContextMenuInfo info;
                        try {
                            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
                        } catch (ClassCastException e) {
                            // Log.e(TAG, "bad menuInfo", e);
                            return;
                        }

                        if (mParentActivity.isRefreshing)
                            return;

                        Object[] args = { R.id.row_post_id };

                        try {
                            Method m = android.view.View.class
                                    .getMethod("getTag");
                            m.invoke(mSelectedID, args);
                        } catch (NoSuchMethodException e) {
                            mSelectedID = info.targetView.getId();
                        } catch (IllegalArgumentException e) {
                            mSelectedID = info.targetView.getId();
                        } catch (IllegalAccessException e) {
                            mSelectedID = info.targetView.getId();
                        } catch (InvocationTargetException e) {
                            mSelectedID = info.targetView.getId();
                        }
                        // selectedID = (String)
                        // info.targetView.getTag(R.id.row_post_id);
                        
                        // Show comments menu option only if post allows commenting
                        boolean allowComments = false;
                        Post post = new Post(WordPress.currentBlog
                                .getId(), mSelectedID, isPage);
                        if (post.getId() >= 0) {
                            allowComments = post.isMt_allow_comments();
                        }
                        
                        mRowID = info.position;

                        if (totalDrafts > 0 && mRowID < totalDrafts) {
                            menu.clear();
                            menu.setHeaderTitle(getResources().getText(R.string.draft_actions));
                            menu.add(MENU_GROUP_DRAFTS, MENU_ITEM_EDIT, 0, getResources().getText(R.string.edit_draft));
                            menu.add(MENU_GROUP_DRAFTS, MENU_ITEM_DELETE, 0, getResources().getText(R.string.delete_draft));
                        } else {
                            menu.clear();
                            if (isPage) {
                                menu.setHeaderTitle(getResources().getText(R.string.page_actions));
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_EDIT, 0, getResources().getText(R.string.edit_page));
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_DELETE, 0, getResources().getText( R.string.delete_page));
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_PREVIEW, 0, getResources().getText(R.string.preview_page));
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_SHARE, 0, getResources().getText(R.string.share_url_page));
                                if (allowComments) menu.add(MENU_GROUP_PAGES, MENU_ITEM_ADD_COMMENT, 0, getResources().getText(R.string.add_comment));
                            } else {
                                menu.setHeaderTitle(getResources().getText(R.string.post_actions));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_EDIT, 0, getResources().getText(R.string.edit_post));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_DELETE, 0, getResources().getText(R.string.delete_post));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_PREVIEW, 0, getResources().getText(R.string.preview_post));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_SHARE, 0, getResources().getText(R.string.share_url));
                                if (allowComments) menu.add(MENU_GROUP_POSTS, MENU_ITEM_ADD_COMMENT, 0, getResources().getText(R.string.add_comment));
                            }
                        }
                    }
                });
            }

            if (this.shouldSelectAfterLoad) {
                if (mPostIDs != null) {
                    if (mPostIDs.size() >= 1) {

                        Post post = new Post(WordPress.currentBlog.getId(),
                                Integer.valueOf(mPostIDs.get(0)), isPage);
                        if (post.getId() >= 0) {
                            WordPress.currentPost = post;
                            mOnPostSelectedListener.onPostSelected(post);
                            FragmentManager fm = getActivity().getSupportFragmentManager();
                            ViewPostFragment f = (ViewPostFragment) fm
                                    .findFragmentById(R.id.postDetail);
                            if (f != null && f.isInLayout())
                                getListView().setItemChecked(0, true);
                        }
                    }
                }
                shouldSelectAfterLoad = false;
            }

            if (loadedPosts == null) {
                refreshPosts(false);
            }

            return true;
        } else {

            if (loadedPosts == null) {
                refreshPosts(false);
                if (!isPage)
                    new ApiHelper.RefreshBlogContentTask(getActivity(), WordPress.getCurrentBlog()).execute(false);
            }

            return false;
        }

    }

    private void initArrayList() {
        s_mPostIDs = new ArrayList<String>(mPostIDs);
        s_mTitles = new ArrayList<String>(mTitles);
        s_mStatuses = new ArrayList<String>(mStatuses);
        s_mDateCreatedFormatted = new ArrayList<String>(mDateCreatedFormatted);
    }

    private String getFormattedDate(long localTime) {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        String formattedDate = DateUtils
                .formatDateTime(getActivity().getApplicationContext(),
                        localTime, flags);
        return formattedDate;
    }

    class ViewWrapper {
        View base;
        TextView title = null;
        TextView date = null;
        TextView status = null;

        ViewWrapper(View base) {
            this.base = base;
        }

        TextView getTitle() {
            if (title == null) {
                title = (TextView) base.findViewById(R.id.title);
            }
            return (title);
        }

        TextView getDate() {
            if (date == null) {
                date = (TextView) base.findViewById(R.id.date);
            }
            return (date);
        }

        TextView getStatus() {
            if (status == null) {
                status = (TextView) base.findViewById(R.id.status);
            }
            return (status);
        }
    }

    private boolean loadDrafts() { // loads drafts from the db

        List<Map<String, Object>> loadedPosts;
        if (isPage) {
            loadedPosts = WordPress.wpDB.loadDrafts(
                    WordPress.currentBlog.getId(), true);
        } else {
            loadedPosts = WordPress.wpDB.loadDrafts(
                    WordPress.currentBlog.getId(), false);
        }
        if (loadedPosts != null) {
            mDraftIDs = new ArrayList<String>(loadedPosts.size());
            mDraftTitles = new ArrayList<String>(loadedPosts.size());
            mDraftDateCreated = new ArrayList<String>(loadedPosts.size());
            mUploaded = new int[loadedPosts.size()];
            totalDrafts = loadedPosts.size();
            mDraftStatuses = new ArrayList<String>(loadedPosts.size());

            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                mDraftIDs.add(i, contentHash.get("id").toString());
                mDraftTitles.add(i, StringUtils.unescapeHTML(contentHash.get(
                        "title").toString()));
                mDraftDateCreated.add(i, "");
                mUploaded[i] = (Integer) contentHash.get("uploaded");
                mDraftStatuses.add(i, getString(R.string.local_draft));
            }

            return true;
        } else {
            totalDrafts = 0;
            return false;
        }
    }

    private class PostListAdapter extends BaseAdapter {
        
        public PostListAdapter(Context context) {
        }

        public int getCount() {
            return s_mTitles.size();
        }

        public Object getItem(int position) {
            return position;
        }

        public long getItemId(int position) {
            return position;
        }

        
		public View getView(int position, View convertView, ViewGroup parent) {
            View pv = convertView;
            ViewWrapper wrapper = null;
            if (pv == null) {
                LayoutInflater inflater = getActivity().getLayoutInflater();
                pv = inflater.inflate(R.layout.row_post_page, parent, false);
                wrapper = new ViewWrapper(pv);
                pv.setTag(wrapper);
                wrapper = new ViewWrapper(pv);
                pv.setTag(wrapper);
            } else {
                wrapper = (ViewWrapper) pv.getTag();
            }

            String date = s_mDateCreatedFormatted.get(position);
            String status_text = s_mStatuses.get(position);

            pv.setTag(R.id.row_post_id, s_mPostIDs.get(position));
            pv.setId(Integer.valueOf(s_mPostIDs.get(position)));
            String titleText = s_mTitles.get(position);
            if (titleText.equals(""))
                titleText = "(" + getResources().getText(R.string.untitled) + ")";
            wrapper.getTitle().setText(titleText);
            wrapper.getDate().setText(date);
            wrapper.getStatus().setText(status_text);

            return pv;
        }
		

        public void notifyDataChanged() {
            initArrayList();
            notifyDataSetChanged();
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Post post = new Post(WordPress.currentBlog.getId(), mSelectedID, isPage);

        if (post.getId() < 0) {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager()
                        .beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment
                        .newInstance(getString(R.string.post_not_found));
                alert.show(ft, "alert");
            }
            return false;
        }

        int itemGroupID = item.getGroupId();
        /* Switch on the ID of the item, to get what the user selected. */
        if (itemGroupID == MENU_GROUP_POSTS || itemGroupID == MENU_GROUP_PAGES || itemGroupID == MENU_GROUP_DRAFTS ) {
            switch (item.getItemId()) {
            case MENU_ITEM_EDIT:
                Intent i2 = new Intent(getActivity().getApplicationContext(),
                        EditPostActivity.class);
                i2.putExtra("postID", mSelectedID);
                i2.putExtra("id", WordPress.currentBlog.getId());
                
                if( itemGroupID == MENU_GROUP_PAGES ){ //page synced with the server
                    i2.putExtra("isPage", true);
                } else if ( itemGroupID == MENU_GROUP_DRAFTS ) { //local draft
                    if (isPage) 
                        i2.putExtra("isPage", true);
                    i2.putExtra("localDraft", true);
                }
                
                startActivityForResult(i2, 0);
                return true;
            case MENU_ITEM_DELETE:
                mOnPostActionListener.onPostAction(PostsActivity.POST_DELETE, post);
                return true;
            case MENU_ITEM_PREVIEW:
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                i.putExtra("isPage", itemGroupID == MENU_GROUP_PAGES ? true : false);
                i.putExtra("postID", mSelectedID);
                i.putExtra("blogID", WordPress.currentBlog.getId());
                startActivity(i);
                return true;
            case MENU_ITEM_SHARE:
                mOnPostActionListener.onPostAction(PostsActivity.POST_SHARE, post);
                return true;
            case MENU_ITEM_ADD_COMMENT:
                mOnPostActionListener.onPostAction(PostsActivity.POST_COMMENT, post);
                return true;
            default:
                return false;
            }
        }
        return false;
    }

    public class getRecentPostsTask extends
            AsyncTask<List<?>, Void, Boolean> {

        Context ctx;
        boolean isPage, loadMore;

        protected void onPostExecute(Boolean result) {
            if (isCancelled() || !result) {
                mOnRefreshListener.onRefresh(false);
                if (getActivity() == null)
                    return;
                if (errorMsg != "" && !getActivity().isFinishing()) {
                    FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                    WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newInstance(String.format(getResources().getString(R.string.error_refresh), (isPage) ? getResources().getText(R.string.pages) : getResources().getText(R.string.posts)), errorMsg);
                    try {
                        alert.show(ft, "alert");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    errorMsg = "";
                }
                return;
            }

            if (loadMore)
                switcher.showPrevious();
            mOnRefreshListener.onRefresh(false);
            if (isAdded())
                loadPosts(loadMore);
        }

        @Override
        protected Boolean doInBackground(List<?>... args) {
            boolean success = false;
            List<?> arguments = args[0];
            WordPress.currentBlog = (Blog) arguments.get(0);
            isPage = (Boolean) arguments.get(1);
            int recordCount = (Integer) arguments.get(2);
            loadMore = (Boolean) arguments.get(3);
            XMLRPCClient client = new XMLRPCClient(WordPress.currentBlog.getUrl(),
                    WordPress.currentBlog.getHttpuser(),
                    WordPress.currentBlog.getHttppassword());

            Object[] result = null;
            Object[] params = { WordPress.currentBlog.getBlogId(),
                    WordPress.currentBlog.getUsername(),
                    WordPress.currentBlog.getPassword(), recordCount };
            try {
                result = (Object[]) client.call((isPage) ? "wp.getPages"
                        : "metaWeblog.getRecentPosts", params);
                if (result != null) {
                    if (result.length > 0) {
                        success = true;
                        Map<?, ?> contentHash = new HashMap<Object, Object>();
                        List<Map<?, ?>> dbVector = new Vector<Map<?, ?>>();

                        if (!loadMore) {
                            WordPress.wpDB.deleteUploadedPosts(
                                    WordPress.currentBlog.getId(), isPage);
                        }

                        for (int ctr = 0; ctr < result.length; ctr++) {
                            Map<String, Object> dbValues = new HashMap<String, Object>();
                            contentHash = (Map<?, ?>) result[ctr];
                            dbValues.put("blogID",
                                    WordPress.currentBlog.getBlogId());
                            dbVector.add(ctr, contentHash);
                        }

                        WordPress.wpDB.savePosts(dbVector,
                                WordPress.currentBlog.getId(), isPage);
                    } else {
                        if (mPostListAdapter != null) {
                            if (mPostIDs.size() == 2) {
                                try {
                                    WordPress.wpDB.deleteUploadedPosts(
                                            WordPress.currentBlog.getId(),
                                            WordPress.currentPost.isPage());
                                    mOnPostActionListener
                                    .onPostAction(PostsActivity.POST_CLEAR,
                                            WordPress.currentPost);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                WordPress.currentPost = null;
                            }
                        }
                    }
                }
            } catch (XMLRPCException e) {
                errorMsg = e.getMessage();
                if (errorMsg == null)
                    errorMsg = getResources().getString(R.string.error_generic);
            }

            return success;
        }
    }
    
    public void filter(String query){
        String charText = query.toLowerCase();
        s_mPostIDs.clear();
        s_mTitles.clear();
        s_mStatuses.clear();
        s_mDateCreatedFormatted.clear();
        s_mPostIDs = new ArrayList<String>();
        s_mTitles = new ArrayList<String>();
        s_mStatuses = new ArrayList<String>();
        s_mDateCreatedFormatted = new ArrayList<String>();
        if (charText.length() == 0) {
            s_mPostIDs.addAll(mPostIDs);
            s_mTitles.addAll(mTitles);
            s_mStatuses.addAll(mStatuses);
            s_mDateCreatedFormatted.addAll(mDateCreatedFormatted);
        } else {
            for (int i=0;i<mTitles.size();i++) {
                if(mTitles.get(i).toLowerCase().contains(charText)){
                    s_mPostIDs.add(mPostIDs.get(i));
                    s_mTitles.add(mTitles.get(i));
                    s_mStatuses.add(mStatuses.get(i));
                    s_mDateCreatedFormatted.add(mDateCreatedFormatted.get(i));
                }
            }
        }
        mPostListAdapter.notifyDataSetChanged();
    }
    
    public void closeSearch(){
        s_mPostIDs.addAll(mPostIDs);
        s_mTitles.addAll(mTitles);
        s_mStatuses.addAll(mStatuses);
        s_mDateCreatedFormatted.addAll(mDateCreatedFormatted);
        mPostListAdapter.notifyDataSetChanged();
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnRefreshListener {
        public void onRefresh(boolean start);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

}
