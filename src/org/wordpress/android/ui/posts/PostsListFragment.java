package org.wordpress.android.ui.posts;

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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ListScrollPositionManager;
import org.wordpress.android.util.PostUploadService;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.wordpress.android.util.WPMobileStatsUtil;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCClient;
import org.xmlrpc.android.XMLRPCException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class PostsListFragment extends ListFragment {
    private String[] mPostIDs, mTitles, mDateCreated, mDateCreatedFormatted,
            mDraftIDs, mDraftTitles, mDraftDateCreated, mStatuses, mDraftStatuses;
    private int[] mUploaded;
    private int mRowID = 0;
    private long mSelectedID = -1;
    private PostListAdapter mPostListAdapter;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnRefreshListener mOnRefreshListener;
    private OnPostActionListener mOnPostActionListener;
    private PostsActivity mParentActivity;
    private ListScrollPositionManager mListScrollPositionManager;
    private int mLoadedBlogId;

    public List<String> imageUrl = new Vector<String>();
    public int totalDrafts = 0;
    public boolean isPage = false, shouldSelectAfterLoad = false;
    public int numRecords = 20;
    public ViewSwitcher switcher;
    public getRecentPostsTask getPostsTask;

    private static final int MENU_GROUP_PAGES = 2, MENU_GROUP_POSTS = 0, MENU_GROUP_DRAFTS = 1;
    private static final int MENU_ITEM_EDIT = 0, MENU_ITEM_DELETE = 1, MENU_ITEM_PREVIEW = 2, MENU_ITEM_SHARE = 3;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            isPage = extras.getBoolean("viewPages");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.post_listview, container, false);
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        TextView textview = (TextView) getActivity().findViewById(R.id.title_empty);
        if (textview != null) {
            if (isPage) {
                textview.setText(getText(R.string.pages_empty_list));
            } else {
                textview.setText(getText(R.string.posts_empty_list));
            }
        }
        createSwitcher();
        mListScrollPositionManager = new ListScrollPositionManager(getListView(), true);
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
        Blog currentBlog = WordPress.getCurrentBlog();
        if (currentBlog != null && mLoadedBlogId != currentBlog.getRemoteBlogId()) {
            WordPress.currentPost = null;
            loadPosts(false);
        }
    }

    public void createSwitcher() {
        // create the ViewSwitcher in the current context
        switcher = new ViewSwitcher(getActivity().getApplicationContext());
        Button footer = (Button) View.inflate(getActivity()
                .getApplicationContext(), R.layout.list_footer_btn, null);
        footer.setText(getResources().getText(R.string.load_more) + " "
                + getResources().getText((isPage) ? R.string.tab_pages : R.string.tab_posts));

        footer.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                if (!WordPress.wpDB.findLocalChanges(WordPress.getCurrentBlog().getLocalTableBlogId(), isPage)) {
                    // first view is showing, show the second progress view
                    switcher.showNext();
                    // get 20 more posts
                    numRecords += 20;
                    refreshPosts(true);
                } else {
                    if (!getActivity().isFinishing()) {
                        FragmentTransaction ft = getFragmentManager().beginTransaction();
                        WPAlertDialogFragment alert = WPAlertDialogFragment.newConfirmDialog(
                                getString(R.string.local_changes),
                                getString(R.string.remote_changes));
                        ft.add(alert, "alert");
                        ft.commitAllowingStateLoss();
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
        mListScrollPositionManager.saveScrollOffset();
        if (!loadMore) {
            mOnRefreshListener.onRefresh(true);
            numRecords = 20;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(isPage);
        apiArgs.add(numRecords);
        apiArgs.add(loadMore);
        getPostsTask = new getRecentPostsTask();
        getPostsTask.execute(apiArgs);
    }

    public boolean loadPosts(boolean loadMore) { // loads posts from the db
        List<Map<String, Object>> loadedPosts;
        if (WordPress.currentBlog != null) {
            mLoadedBlogId = WordPress.currentBlog.getRemoteBlogId();
        }
        try {
            loadedPosts = WordPress.wpDB.loadUploadedPosts(WordPress.currentBlog.getLocalTableBlogId(), isPage);
        } catch (Exception e1) {
            return false;
        }

        if (loadedPosts != null) {
            numRecords = loadedPosts.size();
            mTitles = new String[loadedPosts.size()];
            mPostIDs = new String[loadedPosts.size()];
            mDateCreated = new String[loadedPosts.size()];
            mDateCreatedFormatted = new String[loadedPosts.size()];
            mStatuses = new String[loadedPosts.size()];
        } else {
            mTitles = new String[0];
            mPostIDs = new String[0];
            mDateCreated = new String[0];
            mDateCreatedFormatted = new String[0];
            mStatuses = new String[0];
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataSetChanged();
                if (WordPress.currentPost != null) {
                    mOnPostActionListener.onPostAction(PostsActivity.POST_CLEAR, WordPress.currentPost);
                    WordPress.currentPost = null;
                }
            }
        }
        if (loadedPosts != null) {
            Date d = new Date();
            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                mTitles[i] = StringUtils.unescapeHTML(contentHash.get("title").toString());

                mPostIDs[i] = contentHash.get("id").toString();
                mDateCreated[i] = contentHash.get("date_created_gmt").toString();

                if (contentHash.get("post_status") != null) {
                    String api_status = contentHash.get("post_status").toString();
                    if (api_status.equals("publish")) {
                        mStatuses[i] = getResources().getText(R.string.published).toString();
                    } else if (api_status.equals("draft")) {
                        mStatuses[i] = getResources().getText(R.string.draft).toString();
                    } else if (api_status.equals("pending")) {
                        mStatuses[i] = getResources().getText(R.string.pending_review).toString();
                    } else if (api_status.equals("private")) {
                        mStatuses[i] = getResources().getText(R.string.post_private).toString();
                    }

                    if ((Long) contentHash.get("date_created_gmt") > d.getTime() && api_status.equals("publish")) {
                        mStatuses[i] = getResources().getText(R.string.scheduled).toString();
                    }
                }

                long localTime = (Long) contentHash.get("date_created_gmt");
                mDateCreatedFormatted[i] = getFormattedDate(localTime);
            }
        }
        // load drafts
        boolean drafts = loadDrafts();

        if (drafts) {
            mPostIDs = StringUtils.mergeStringArrays(mDraftIDs, mPostIDs);
            mTitles = StringUtils.mergeStringArrays(mDraftTitles, mTitles);
            mDateCreatedFormatted = StringUtils.mergeStringArrays(
                    mDraftDateCreated, mDateCreatedFormatted);
            mStatuses = StringUtils.mergeStringArrays(mDraftStatuses, mStatuses);
        } else {
            if (mPostListAdapter != null) {
                mPostListAdapter.notifyDataSetChanged();
            }
        }

        if (loadedPosts != null || drafts == true) {
            ListView listView = getListView();
            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            listView.setDivider(getResources().getDrawable(R.drawable.list_divider));
            listView.setDividerHeight(1);
            listView.removeFooterView(switcher);
            if (loadedPosts != null) {
                if (loadedPosts.size() >= 20) {
                    listView.addFooterView(switcher);
                }
            }

            if (loadMore) {
                mPostListAdapter.notifyDataSetChanged();
            } else {
                mPostListAdapter = new PostListAdapter(getActivity().getBaseContext());
                listView.setAdapter(mPostListAdapter);

                listView.setOnItemClickListener(new OnItemClickListener() {
                    public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                        if (position >= mPostIDs.length) //out of bounds
                            return;
                        if (v == null) //view is gone
                            return;
                        if (!mParentActivity.mIsRefreshing) {
                            mSelectedID = v.getId();
                            showPost(mSelectedID);
                        } else {
                            Toast.makeText(mParentActivity, R.string.please_wait_refresh_done,
                                    Toast.LENGTH_SHORT).show();
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
                            AppLog.e(T.POSTS, "bad menuInfo", e);
                            return;
                        }

                        if (mParentActivity.mIsRefreshing)
                            return;

                        Object[] args = {R.id.row_post_id};

                        try {
                            Method m = android.view.View.class.getMethod("getTag");
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
                                .getLocalTableBlogId(), mSelectedID, isPage);
                        if (post.getId() >= 0) {
                            allowComments = post.isMt_allow_comments();
                        }

                        if (PostUploadService.isUploading(post)) {
                            return;
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
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_DELETE, 0, getResources().getText(R.string.delete_page));
                                menu.add(MENU_GROUP_PAGES, MENU_ITEM_PREVIEW, 0, getResources().getText(R.string.preview_page));
                                // Post status: publish, draft, pending, private, localdraft
                                if ("publish".equals(post.getPost_status())) {
                                    menu.add(MENU_GROUP_PAGES, MENU_ITEM_SHARE, 0, getResources().getText(R.string.share_url_page));
                                }
                            } else {
                                menu.setHeaderTitle(getResources().getText(R.string.post_actions));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_EDIT, 0, getResources().getText(R.string.edit_post));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_DELETE, 0, getResources().getText(R.string.delete_post));
                                menu.add(MENU_GROUP_POSTS, MENU_ITEM_PREVIEW, 0, getResources().getText(R.string.preview_post));
                                if ("publish".equals(post.getPost_status())) {
                                    menu.add(MENU_GROUP_POSTS, MENU_ITEM_SHARE, 0, getResources().getText(R.string.share_url));
                                }
                            }
                        }
                    }
                });
            }

            if (this.shouldSelectAfterLoad && mPostIDs != null && mPostIDs.length >= 1) {
                selectAndShowFirstPost();
                shouldSelectAfterLoad = false;
            }

            if (loadedPosts == null) {
                refreshPosts(false);
            }
            mListScrollPositionManager.restoreScrollOffset();
            return true;
        } else {

            if (loadedPosts == null) {
                refreshPosts(false);
                if (!isPage)
                    new ApiHelper.RefreshBlogContentTask(getActivity(), WordPress.getCurrentBlog(), new ApiHelper.VerifyCredentialsCallback(getActivity())).execute(false);
            }
            mListScrollPositionManager.restoreScrollOffset();
            return false;
        }
    }

    private String getFormattedDate(long localTime) {
        int flags = 0;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_DATE;
        flags |= android.text.format.DateUtils.FORMAT_ABBREV_MONTH;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_YEAR;
        flags |= android.text.format.DateUtils.FORMAT_SHOW_TIME;
        String formattedDate = DateUtils.formatDateTime(getActivity().getApplicationContext(), localTime, flags);
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
                    WordPress.currentBlog.getLocalTableBlogId(), true);
        } else {
            loadedPosts = WordPress.wpDB.loadDrafts(
                    WordPress.currentBlog.getLocalTableBlogId(), false);
        }
        if (loadedPosts != null) {
            mDraftIDs = new String[loadedPosts.size()];
            mDraftTitles = new String[loadedPosts.size()];
            mDraftDateCreated = new String[loadedPosts.size()];
            mUploaded = new int[loadedPosts.size()];
            totalDrafts = loadedPosts.size();
            mDraftStatuses = new String[loadedPosts.size()];

            for (int i = 0; i < loadedPosts.size(); i++) {
                Map<String, Object> contentHash = loadedPosts.get(i);
                mDraftIDs[i] = contentHash.get("id").toString();
                mDraftTitles[i] = StringUtils.unescapeHTML(contentHash.get(
                        "title").toString());
                mDraftDateCreated[i] = "";
                mUploaded[i] = (Integer) contentHash.get("uploaded");
                mDraftStatuses[i] = getString(R.string.local_draft);
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
            return mPostIDs.length;
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

            String date = mDateCreatedFormatted[position];
            String status_text = mStatuses[position];

            pv.setTag(R.id.row_post_id, mPostIDs[position]);
            pv.setId(Integer.valueOf(mPostIDs[position]));
            String titleText = mTitles[position];
            if (titleText.equals(""))
                titleText = "(" + getResources().getText(R.string.untitled) + ")";
            wrapper.getTitle().setText(titleText);
            wrapper.getDate().setText(date);
            wrapper.getStatus().setText(status_text);

            return pv;
        }

    }

    public String statEventForViewClosing() {
        if (isPage) {
            return WPMobileStatsUtil.StatsEventPagesClosed;
        } else {
            return WPMobileStatsUtil.StatsEventPostsClosed;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Post post = new Post(WordPress.currentBlog.getLocalTableBlogId(), mSelectedID, isPage);

        if (post.getId() < 0) {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
            return false;
        }

        int itemGroupID = item.getGroupId();
        /* Switch on the ID of the item, to get what the user selected. */
        if (itemGroupID == MENU_GROUP_POSTS || itemGroupID == MENU_GROUP_PAGES || itemGroupID == MENU_GROUP_DRAFTS ) {
            switch (item.getItemId()) {
            case MENU_ITEM_EDIT:
                WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostMenuClickedEdit);
                Intent i2 = new Intent(getActivity().getApplicationContext(), EditPostActivity.class);
                i2.putExtra(EditPostActivity.EXTRA_POSTID, mSelectedID);
                if( itemGroupID == MENU_GROUP_PAGES ){ //page synced with the server
                    i2.putExtra(EditPostActivity.EXTRA_IS_PAGE, true);
                } else if ( itemGroupID == MENU_GROUP_DRAFTS ) { //local draft
                    if (isPage)
                        i2.putExtra(EditPostActivity.EXTRA_IS_PAGE, true);
                }

                getActivity().startActivityForResult(i2, PostsActivity.ACTIVITY_EDIT_POST);
                return true;
            case MENU_ITEM_DELETE:
                WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostMenuClickedDelete);
                mOnPostActionListener.onPostAction(PostsActivity.POST_DELETE, post);
                return true;
            case MENU_ITEM_PREVIEW:
                WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostMenuClickedPreview);
                Intent i = new Intent(getActivity(), PreviewPostActivity.class);
                i.putExtra("isPage", itemGroupID == MENU_GROUP_PAGES ? true : false);
                i.putExtra("postID", mSelectedID);
                i.putExtra("blogID", WordPress.currentBlog.getLocalTableBlogId());
                startActivity(i);
                return true;
            case MENU_ITEM_SHARE:
                WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostMenuClickedShare);
                mOnPostActionListener.onPostAction(PostsActivity.POST_SHARE, post);
                return true;
            /*case MENU_ITEM_ADD_COMMENT:
                WPMobileStatsUtil.flagProperty(statEventForViewClosing(), WPMobileStatsUtil.StatsPropertyPostMenuClickedComment);
                mOnPostActionListener.onPostAction(PostsActivity.POST_COMMENT, post);
                return true;*/
            default:
                return false;
            }
        }
        return false;
    }

    public class getRecentPostsTask extends
            AsyncTask<List<?>, Void, Boolean> {

        private boolean mIsPage, mLoadMore;
        private Blog mBlog;

        protected void onPostExecute(Boolean result) {
            if (isCancelled() || !result) {
                mOnRefreshListener.onRefresh(false);
                return;
            }

            if (mLoadMore)
                switcher.showPrevious();
            mOnRefreshListener.onRefresh(false);
            if (isAdded()) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                        loadPosts(mLoadMore);
                        }
                    });
                }
            }
        }

        @Override
        protected Boolean doInBackground(List<?>... args) {
            boolean success = false;
            List<?> arguments = args[0];
            mBlog = (Blog) arguments.get(0);
            if (mBlog == null)
                return false;
            mIsPage = (Boolean) arguments.get(1);
            int recordCount = (Integer) arguments.get(2);
            mLoadMore = (Boolean) arguments.get(3);
            XMLRPCClient client = new XMLRPCClient(mBlog.getUrl(),
                    mBlog.getHttpuser(),
                    mBlog.getHttppassword());
            Object[] params = {mBlog.getRemoteBlogId(),
                    mBlog.getUsername(),
                    mBlog.getPassword(), recordCount};
            try {
                Object[] result = (Object[]) client.call((mIsPage) ? "wp.getPages"
                        : "metaWeblog.getRecentPosts", params);
                if (result != null) {
                    if (result.length > 0) {
                        success = true;
                        List<Map<?, ?>> postsList = new ArrayList<Map<?, ?>>();
                        if (!mLoadMore) {
                            WordPress.wpDB.deleteUploadedPosts(mBlog.getLocalTableBlogId(), mIsPage);
                        }
                        for (int ctr = 0; ctr < result.length; ctr++) {
                            Map<?, ?> postMap = (Map<?, ?>) result[ctr];
                            postsList.add(ctr, postMap);
                        }
                        WordPress.wpDB.savePosts(postsList, mBlog.getLocalTableBlogId(), mIsPage);
                    }
                }
            } catch (XMLRPCException e) {
            }

            return success;
        }
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

    private void showPost(long selectedID) {
        Post post = new Post(WordPress.currentBlog.getLocalTableBlogId(), selectedID, isPage);
        if (post.getId() >= 0) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
            mPostListAdapter.notifyDataSetChanged();
        } else {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
        }
    }

    private void selectAndShowFirstPost() {
        Post post = new Post(WordPress.currentBlog.getLocalTableBlogId(), Integer.valueOf(mPostIDs[0]), isPage);
        if (post.getId() >= 0) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
            FragmentManager fm = getActivity().getSupportFragmentManager();
            ViewPostFragment f = (ViewPostFragment) fm.findFragmentById(R.id.postDetail);
            if (f != null && f.isInLayout()) {
                getListView().setItemChecked(0, true);
            }
        }
    }
}
