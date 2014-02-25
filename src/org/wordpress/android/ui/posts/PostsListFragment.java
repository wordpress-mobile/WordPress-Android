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
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.adapters.PostListAdapter;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;

import java.util.List;
import java.util.Vector;

public class PostsListFragment extends ListFragment {
    /** Called when the activity is first created. */
    private OnPostSelectedListener mOnPostSelectedListener;
    private OnRefreshListener mOnRefreshListener;
    private PostsActivity mParentActivity;
    private PostListAdapter mPostListAdapter;
    private View mProgressFooterView;
    private boolean mCanLoadMorePosts = true;

    private boolean mIsPage;

    public static final int POSTS_REQUEST_COUNT = 20;

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
        View v = inflater.inflate(R.layout.empty_listview, container, false);
        return v;
    }

    public PostListAdapter getPostListAdapter() {
        if (mPostListAdapter == null) {
            PostListAdapter.OnLoadMoreListener loadMoreListener = new PostListAdapter.OnLoadMoreListener() {
                @Override
                public void onLoadMore(boolean loadMore) {
                    if (mCanLoadMorePosts)
                        requestPosts(loadMore);
                }
            };

            mPostListAdapter = new PostListAdapter(getActivity(), mIsPage, loadMoreListener);
        }

        return mPostListAdapter;
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
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
                if (!mParentActivity.mIsRefreshing) {
                    showPost(postsListPost.getPostId());
                } else {
                    Toast.makeText(mParentActivity, R.string.please_wait_refresh_done,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

        TextView textview = (TextView) getListView().getEmptyView();
        if (textview != null) {
            if (mIsPage) {
                textview.setText(getText(R.string.pages_empty_list));
            } else {
                textview.setText(getText(R.string.posts_empty_list));
            }
            textview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mParentActivity.newPost();
                }
            });
        }
    }

    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            // check that the containing activity implements our callback
            mOnPostSelectedListener = (OnPostSelectedListener) activity;
            mOnRefreshListener = (OnRefreshListener) activity;
        } catch (ClassCastException e) {
            activity.finish();
            throw new ClassCastException(activity.toString()
                    + " must implement Callback");
        }
    }

    public void onResume() {
        super.onResume();
        mParentActivity = (PostsActivity) getActivity();
        if (WordPress.getCurrentBlog() != null) {
            getListView().setAdapter(getPostListAdapter());
            getPostListAdapter().loadPosts();
        }
    }

    private void showPost(long selectedID) {
        Post post = new Post(WordPress.currentBlog.getLocalTableBlogId(), selectedID, mIsPage);
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

    public void requestPosts(boolean loadMore) {
        int postCount = getPostListAdapter().getRemotePostCount() + POSTS_REQUEST_COUNT;
        if (!loadMore) {
            mOnRefreshListener.onRefresh(true);
            postCount = POSTS_REQUEST_COUNT;
        }
        List<Object> apiArgs = new Vector<Object>();
        apiArgs.add(WordPress.getCurrentBlog());
        apiArgs.add(mIsPage);
        apiArgs.add(postCount);
        apiArgs.add(loadMore);
        if (mProgressFooterView != null && loadMore)
            mProgressFooterView.setVisibility(View.VISIBLE);

        ApiHelper.FetchPostsTask fetchPostsTaskTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                if (!hasActivity())
                    return;
                mOnRefreshListener.onRefresh(false);
                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                    return;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    // TODO: What if a user has exactly POSTS_REQUESTS_COUNT posts on their blog?
                    mCanLoadMorePosts = false;
                    if (mProgressFooterView != null) {
                        mProgressFooterView.setVisibility(View.GONE);
                    }
                    return;
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                if (!hasActivity())
                    return;
                mOnRefreshListener.onRefresh(false);
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

        fetchPostsTaskTask.execute(apiArgs);
    }

    protected void clear() {
        if (getPostListAdapter() != null)
            getPostListAdapter().clear();
    }

    private boolean hasActivity() {
        return getActivity() != null;
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
