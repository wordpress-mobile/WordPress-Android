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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Post;
import org.wordpress.android.models.PostsListPost;
import org.wordpress.android.ui.posts.adapters.PostsListAdapter;
import org.wordpress.android.util.WPAlertDialogFragment;
import org.xmlrpc.android.ApiHelper;

import java.util.List;
import java.util.Vector;

public class PostsListFragment extends ListFragment {

    public static final int POSTS_REQUEST_COUNT = 20;

    private OnPostSelectedListener mOnPostSelectedListener;
    private OnRefreshListener mOnRefreshListener;
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
        return inflater.inflate(R.layout.empty_listview, container, false);
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
                    } else if (mShouldSelectFirstPost) {
                        // Select the first row on a tablet, if available
                        mShouldSelectFirstPost = false;
                        if (mPostsListAdapter.getCount() > 0) {
                            PostsListPost postsListPost = (PostsListPost) mPostsListAdapter.getItem(0);
                            if (postsListPost != null) {
                                showPost(postsListPost.getPostId());
                                getListView().setItemChecked(0, true);
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
                if (!mIsFetchingPosts) {
                    showPost(postsListPost.getPostId());
                } else if (hasActivity()) {
                    Toast.makeText(getActivity(), R.string.please_wait_refresh_done,
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
                    if (hasActivity()) {
                        PostsActivity postsActivity = (PostsActivity) getActivity();
                        postsActivity.newPost();
                    }
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
        if (WordPress.getCurrentBlog() != null) {
            if (getListView().getAdapter() == null) {
                getListView().setAdapter(getPostListAdapter());
            }

            getPostListAdapter().loadPosts();
        }
    }

    private void showPost(long selectedID) {
        if (WordPress.getCurrentBlog() == null)
            return;

        Post post = new Post(WordPress.getCurrentLocalTableBlogId(), selectedID, mIsPage);
        if (post.getId() >= 0) {
            WordPress.currentPost = post;
            mOnPostSelectedListener.onPostSelected(post);
            mPostsListAdapter.notifyDataSetChanged();
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
        if (WordPress.getCurrentBlog() == null)
            return;

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
        if (mProgressFooterView != null && loadMore) {
            mProgressFooterView.setVisibility(View.VISIBLE);
        }

        ApiHelper.FetchPostsTask fetchPostsTaskTask = new ApiHelper.FetchPostsTask(new ApiHelper.FetchPostsTask.Callback() {
            @Override
            public void onSuccess(int postCount) {
                mIsFetchingPosts = false;
                if (!hasActivity())
                    return;
                mOnRefreshListener.onRefresh(false);
                if (postCount == 0) {
                    mCanLoadMorePosts = false;
                } else if (postCount == getPostListAdapter().getRemotePostCount() && postCount != POSTS_REQUEST_COUNT) {
                    // TODO: What if a user has exactly POSTS_REQUESTS_COUNT posts on their blog?
                    mCanLoadMorePosts = false;
                    if (mProgressFooterView != null) {
                        mProgressFooterView.setVisibility(View.GONE);
                    }
                }

                getPostListAdapter().loadPosts();
            }

            @Override
            public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
                mIsFetchingPosts = false;
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

        mIsFetchingPosts = true;
        fetchPostsTaskTask.execute(apiArgs);
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
