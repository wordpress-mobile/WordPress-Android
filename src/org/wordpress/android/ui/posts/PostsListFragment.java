package org.wordpress.android.ui.posts;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.Utils;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;

import java.util.List;
import java.util.Vector;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

public class PostsListFragment extends ListFragment implements WordPress.OnPostUploadedListener {

    public static final int POSTS_REQUEST_COUNT = 20;

    private PullToRefreshHelper mPullToRefreshHelper;
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnSinglePostLoadedListener mOnSinglePostLoadedListener;
    private PostsListAdapter mPostsListAdapter;
    private View mProgressFooterView;
    private boolean mCanLoadMorePosts = true;
    private boolean mIsPage, mShouldSelectFirstPost, mIsFetchingPosts;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Bundle extras = getActivity().getIntent().getExtras();
        if (extras != null) {
            mIsPage = extras.getBoolean(PostsActivity.EXTRA_VIEW_PAGES);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.post_listview, container, false);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(getActivity(),
                (PullToRefreshLayout) view.findViewById(R.id.ptr_layout), new RefreshListener() {
            @Override
            public void onRefreshStarted(View view) {
                if (getActivity() == null || !NetworkUtils.checkConnection(getActivity())) {
                    mPullToRefreshHelper.setRefreshing(false);
                    return;
                }
                PostsActivity postsActivity = (PostsActivity) getActivity();
                postsActivity.checkForLocalChanges(true);
                Blog currentBlog = WordPress.getCurrentBlog();
                new ApiHelper.RefreshBlogContentTask(postsActivity, currentBlog,
                        new ApiHelper.VerifyCredentialsCallback(postsActivity, currentBlog.isDotcomFlag())
                ).execute(false);
            }
        }, LinearLayout.class
        );
        return view;
    }

    public PostsListAdapter getPostListAdapter() {
        if (mPostsListAdapter == null) {
            PostsListAdapter.OnLoadMoreListener loadMoreListener = new PostsListAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore() {
                    if (mCanLoadMorePosts && !mIsFetchingPosts)
                        requestPosts(true);
                }
            };

            PostsListAdapter.OnPostsLoadedListener postsLoadedListener = new PostsListAdapter.OnPostsLoadedListener() {
                @Override
                public void onPostsLoaded(int postCount) {
                    if (postCount == 0 && mCanLoadMorePosts) {
                        // No posts, let's request some
                        requestPosts(false);
                        setRefreshing(true);
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if requested
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getCount() > 0) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                                getListView().setItemChecked(0, true);
                            }
                        }
                    } else if (Utils.isTablet()) {
                        // Reload the last selected position, if available
                        int selectedPosition = getListView().getCheckedItemPosition();
                        if (selectedPosition != ListView.INVALID_POSITION && selectedPosition < mPostsListAdapter.getCount()) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(selectedPosition);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                            }
                        }
                    }
                }
            };

            mPostsListAdapter = new PostsListAdapter(getActivity(), mIsPage, loadMoreListener, postsLoadedListener);
        }

        return mPostsListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mProgressFooterView = View.inflate(getActivity(), R.layout.list_footer_progress, null);
        getListView().addFooterView(mProgressFooterView, null, false);
        mProgressFooterView.setVisibility(View.GONE);
        getListView().setDivider(getResources().getDrawable(R.drawable.list_divider));
        getListView().setDividerHeight(1);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                if (position >= getPostListAdapter().getCount()) //out of bounds
                    return;
                if (v == null) //view is gone
                    return;
                PostsListPost postsListPost = (PostsListPost) getPostListAdapter().getItem(position);
                if (postsListPost == null)
                    return;
                if (!mIsFetchingPosts || isLoadingMorePosts()) {
                    showPost(postsListPost.getPostId());
                } else if (hasActivity()) {
                    Toast.makeText(getActivity(), mIsPage ? R.string.loading_pages : R.string.loading_posts,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView textView = (TextView) getActivity().findViewById(R.id.title_empty);
        if (textView != null) {
            if (mIsPage) {
                textView.setText(getText(R.string.pages_empty_list));
            } else {
                textView.setText(getText(R.string.posts_empty_list));
            }
        }

        WordPress.setOnPostUploadedListener(this);
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnSinglePostLoadedListener = (OnSinglePostLoadedListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();
        if (WordPress.getCurrentBlog() != null) {
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getPostListAdapter());
            }

            getPostListAdapter().loadPosts();
        }
    }

    public void setRefreshing(boolean refreshing) {
        mPullToRefreshHelper.setRefreshing(refreshing);
    }

    private void showPost(long selectedId) {
        if (WordPress.getCurrentBlog() == null)
            return;

        Post post = WordPress.wpDB.getPostForLocalTablePostId(selectedId);
        if (post != null) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
        } else {
            if (!getActivity().isFinishing()) {
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                WPAlertDialogFragment alert = WPAlertDialogFragment.newAlertDialog(getString(R.string.post_not_found));
                ft.add(alert, "alert");
                ft.commitAllowingStateLoss();
            }
        }
    }

    public boolean isLoadingMorePosts() {
        return mIsFetchingPosts && (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE);
    }

    public void requestPosts(boolean loadMore) {
        if (!hasActivity() || WordPress.getCurrentBlog() == null || mIsFetchingPosts)
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        int postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
        if (!loadMore) {
            mCanLoadMorePosts = true;
            postCount = POSTS_REQUEST_COUNT;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);
        if (mProgressFooterView != null && loadMore) {
            mProgressFooterView.setVisibility(View.VISIBLE);
        }

        ApiHelper.FetchPostsTask fetchPostsTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mIsFetchingPosts = false;
                if (!hasActivity())
                    return;
                mPullToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null) {
                    mProgressFooterView.setVisibility(View.GONE);
                }

                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    mCanLoadMorePosts = false;
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mIsFetchingPosts = false;
                if (!hasActivity())
                    return;
                mPullToRefreshHelper.setRefreshing(false);
                if (mProgressFooterView != null)
                    mProgressFooterView.setVisibility(View.GONE);

                if (!TextUtils.isEmpty(errorMessage) && !getActivity().isFinishing()) {
                    FragmentTransaction ft = getFragmentManager()
                            .beginTransaction();
                    WPAlertDialogFragment alert = WPAlertDialogFragment
                            .newAlertDialog(mIsPage ? getString(R.string.error_refresh_pages) : getString(R.string.error_refresh_posts));
                    try {
                        alert.show(ft, "alert");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mIsFetchingPosts = true;
        fetchPostsTask.execute(apiArgs);
    }

    protected void clear() {
        if (getPostListAdapter() != null) {
            getPostListAdapter().clear();
        }
        mCanLoadMorePosts = true;
        if (mProgressFooterView != null && mProgressFooterView.getVisibility() == View.VISIBLE) {
            mProgressFooterView.setVisibility(View.GONE);
        }
    }

    public void setShouldSelectFirstPost(boolean shouldSelect) {
        mShouldSelectFirstPost = shouldSelect;
    }

    private boolean hasActivity() {
        return getActivity() != null;
    }

    @Override
    public void OnPostUploaded(String postId) {
        if (!hasActivity())
            return;

        if (!NetworkUtils.checkConnection(getActivity()))
            return;

        // Fetch the newly uploaded post
        if (!TextUtils.isEmpty(postId)) {
            List<Object> apiArgs = new Vector<Object>();
            apiArgs.add(WordPress.getCurrentBlog());
            apiArgs.add(postId);
            apiArgs.add(mIsPage);

            ApiHelper.FetchSinglePostTask fetchPostTask = new ApiHelper.FetchSinglePostTask(new ApiHelper.FetchSinglePostTask.Callback() {
                @Override
                public void onSuccess() {
                    if (!hasActivity())
                        return;

                    mIsFetchingPosts = false;
                    mPullToRefreshHelper.setRefreshing(false);
                    getPostListAdapter().loadPosts();
                    mOnSinglePostLoadedListener.onSinglePostLoaded();
                }

                @Override
                public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                    if (!hasActivity())
                        return;

                    Toast.makeText(getActivity(), R.string.error_refresh_posts, Toast.LENGTH_SHORT).show();
                    mIsFetchingPosts = false;
                    mPullToRefreshHelper.setRefreshing(false);
                }
            });

            mPullToRefreshHelper.setRefreshing(true);
            mIsFetchingPosts = true;
            fetchPostTask.execute(apiArgs);
        }
    }

    public interface OnPostSelectedListener {
        public void onPostSelected(Post post);
    }

    public interface OnPostActionListener {
        public void onPostAction(int action, Post post);
    }

    public interface OnSinglePostLoadedListener {
        public void onSinglePostLoaded();
    }
}
