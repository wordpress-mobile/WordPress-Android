package org.wordpress.android.ui.people;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.Person;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PeopleListFragment extends Fragment {
    private static final String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";

    private int mLocalTableBlogID;
    private OnPersonSelectedListener mOnPersonSelectedListener;
    private OnFetchPeopleListener mOnFetchPeopleListener;

    private FilteredRecyclerView mFilteredRecyclerView;
    private PeopleListFilter mPeopleListFilter;

    public static PeopleListFragment newInstance(int localTableBlogID) {
        PeopleListFragment peopleListFragment = new PeopleListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        peopleListFragment.setArguments(bundle);
        return peopleListFragment;
    }

    public void setOnPersonSelectedListener(OnPersonSelectedListener listener) {
        mOnPersonSelectedListener = listener;
    }

    public void setOnFetchPeopleListener(OnFetchPeopleListener listener) {
        mOnFetchPeopleListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnPersonSelectedListener = null;
        mOnFetchPeopleListener = null;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.people_list, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_list_fragment, container, false);

        mLocalTableBlogID = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);

        mFilteredRecyclerView = (FilteredRecyclerView) rootView.findViewById(R.id.filtered_recycler_view);
        mFilteredRecyclerView.addItemDecoration(new PeopleItemDecoration(getActivity(), R.drawable.people_list_divider));
        mFilteredRecyclerView.setLogT(AppLog.T.PEOPLE);
        mFilteredRecyclerView.setSwipeToRefreshEnabled(false);

        // the following will change the look and feel of the toolbar to match the current design
        mFilteredRecyclerView.setToolbarBackgroundColor(ContextCompat.getColor(getActivity(), R.color.blue_medium));
        mFilteredRecyclerView.setToolbarSpinnerTextColor(ContextCompat.getColor(getActivity(), R.color.white));
        mFilteredRecyclerView.setToolbarSpinnerDrawable(R.drawable.arrow);
        mFilteredRecyclerView.setToolbarLeftAndRightPadding(
                getResources().getDimensionPixelSize(R.dimen.margin_filter_spinner),
                getResources().getDimensionPixelSize(R.dimen.margin_none));

        mFilteredRecyclerView.setFilterListener(new FilteredRecyclerView.FilterListener() {
            @Override
            public List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh) {
                ArrayList<FilterCriteria> list = new ArrayList<>();
                Collections.addAll(list, PeopleListFilter.values());
                return list;
            }

            @Override
            public void onLoadFilterCriteriaOptionsAsync(FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener, boolean refresh) {
                // no-op
            }

            @Override
            public FilterCriteria onRecallSelection() {
                mPeopleListFilter = AppPrefs.getPeopleListFilter();
                return mPeopleListFilter;
            }

            @Override
            public void onLoadData() {
                updatePeople(false);
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                mPeopleListFilter = (PeopleListFilter) criteria;
                AppPrefs.setPeopleListFilter(mPeopleListFilter);
            }

            @Override
            public String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType) {
                int stringId = 0;
                switch (emptyViewMsgType) {
                    case LOADING:
                        stringId = R.string.people_fetching;
                        break;
                    case NETWORK_ERROR:
                        stringId = R.string.no_network_message;
                        break;
                    case NO_CONTENT:
                        switch (mPeopleListFilter) {
                            case TEAM:
                                stringId = R.string.people_empty_list_filtered_users;
                                break;
                            case FOLLOWERS:
                                stringId = R.string.people_empty_list_filtered_followers;
                                break;
                            case EMAIL_FOLLOWERS:
                                stringId = R.string.people_empty_list_filtered_email_followers;
                                break;
                        }
                        break;
                    case GENERIC_ERROR:
                        switch (mPeopleListFilter) {
                            case TEAM:
                                stringId = R.string.error_fetch_users_list;
                                break;
                            case FOLLOWERS:
                                stringId = R.string.error_fetch_followers_list;
                                break;
                            case EMAIL_FOLLOWERS:
                                stringId = R.string.error_fetch_email_followers_list;
                                break;
                        }
                        break;
                }
                return getString(stringId);
            }

            @Override
            public void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType) {

            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePeople(false);
    }

    private void updatePeople(boolean loadMore) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            mFilteredRecyclerView.updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
            mFilteredRecyclerView.setRefreshing(false);
            return;
        }

        if (mOnFetchPeopleListener != null) {
            if (loadMore) {
                boolean isFetching = mOnFetchPeopleListener.onFetchMorePeople(mPeopleListFilter);
                if (isFetching) {
                    mFilteredRecyclerView.showLoadingProgress();
                }
            } else {
                boolean isFetching = mOnFetchPeopleListener.onFetchFirstPage(mPeopleListFilter);
                if (isFetching) {
                    mFilteredRecyclerView.updateEmptyView(EmptyViewMessageType.LOADING);
                } else {
                    mFilteredRecyclerView.hideEmptyView();
                    mFilteredRecyclerView.setRefreshing(false);
                }
                refreshPeopleList(isFetching);
            }
        }
    }

    public void refreshPeopleList(boolean isFetching) {
        if (!isAdded()) return;

        List<Person> peopleList;
        switch (mPeopleListFilter) {
            case TEAM:
                peopleList = PeopleTable.getUsers(mLocalTableBlogID);
                break;
            case FOLLOWERS:
                peopleList = PeopleTable.getFollowers(mLocalTableBlogID);
                break;
            case EMAIL_FOLLOWERS:
                peopleList = PeopleTable.getEmailFollowers(mLocalTableBlogID);
                break;
            default:
                peopleList = new ArrayList<>();
                break;
        }
        PeopleAdapter peopleAdapter = (PeopleAdapter) mFilteredRecyclerView.getAdapter();
        if (peopleAdapter == null) {
            peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
            mFilteredRecyclerView.setAdapter(peopleAdapter);
        } else {
            peopleAdapter.setPeopleList(peopleList);
        }

        if (!peopleList.isEmpty()) {
            // if the list is not empty, don't show any message
            mFilteredRecyclerView.hideEmptyView();
        } else if (!isFetching) {
            // if we are not fetching and list is empty, show no content message
            mFilteredRecyclerView.updateEmptyView(EmptyViewMessageType.NO_CONTENT);
        }
    }

    public void fetchingRequestFinished(PeopleListFilter filter, boolean isFirstPage, boolean isSuccessful) {
        if (mPeopleListFilter == filter) {
            if (isFirstPage) {
                mFilteredRecyclerView.setRefreshing(false);
                if (!isSuccessful) {
                    mFilteredRecyclerView.updateEmptyView(EmptyViewMessageType.GENERIC_ERROR);
                }
            } else {
                mFilteredRecyclerView.hideLoadingProgress();
            }
        }
    }

    // Container Activity must implement this interface
    public interface OnPersonSelectedListener {
        void onPersonSelected(Person person);
    }

    public interface OnFetchPeopleListener {
        boolean onFetchFirstPage(PeopleListFilter filter);
        boolean onFetchMorePeople(PeopleListFilter filter);
    }

    public class PeopleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final LayoutInflater mInflater;
        private List<Person> mPeopleList;
        private int mAvatarSz;

        public PeopleAdapter(Context context, List<Person> peopleList) {
            mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
            mInflater = LayoutInflater.from(context);
            mPeopleList = peopleList;
            setHasStableIds(true);
        }

        public void setPeopleList(List<Person> peopleList) {
            mPeopleList = peopleList;
            notifyDataSetChanged();
        }

        public Person getPerson(int position) {
            if (mPeopleList == null) {
                return null;
            }
            return mPeopleList.get(position);
        }

        @Override
        public int getItemCount() {
            if (mPeopleList == null) {
                return 0;
            }
            return mPeopleList.size();
        }

        @Override
        public long getItemId(int position) {
            Person person = getPerson(position);
            if (person == null) {
                return -1;
            }
            return person.getPersonID();
        }

        @Override
        public PeopleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.people_list_row, parent, false);

            return new PeopleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            PeopleViewHolder peopleViewHolder = (PeopleViewHolder) holder;
            final Person person = getPerson(position);

            if (person != null) {
                String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), mAvatarSz);
                peopleViewHolder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                peopleViewHolder.txtDisplayName.setText(StringUtils.unescapeHTML(person.getDisplayName()));
                peopleViewHolder.txtRole.setText(StringUtils.capitalize(person.getRole()));
                if (!person.getUsername().isEmpty()) {
                    peopleViewHolder.txtUsername.setVisibility(View.VISIBLE);
                    peopleViewHolder.txtUsername.setText(String.format("@%s", person.getUsername()));
                } else {
                    peopleViewHolder.txtUsername.setVisibility(View.GONE);
                }
                if (person.getPersonType() == Person.PersonType.USER) {
                    peopleViewHolder.txtSubscribed.setVisibility(View.GONE);
                } else {
                    peopleViewHolder.txtSubscribed.setVisibility(View.VISIBLE);
                    String dateSubscribed = SimpleDateFormat.getDateInstance().format(person.getDateSubscribed());
                    String dateText = getString(R.string.follower_subscribed_since, dateSubscribed);
                    peopleViewHolder.txtSubscribed.setText(dateText);
                }
            }

            // end of list is reached
            if (position == getItemCount() - 1) {
                updatePeople(true);
            }
        }

        public class PeopleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final WPNetworkImageView imgAvatar;
            private final TextView txtDisplayName;
            private final TextView txtUsername;
            private final TextView txtRole;
            private final TextView txtSubscribed;

            public PeopleViewHolder(View view) {
                super(view);
                imgAvatar = (WPNetworkImageView) view.findViewById(R.id.person_avatar);
                txtDisplayName = (TextView) view.findViewById(R.id.person_display_name);
                txtUsername = (TextView) view.findViewById(R.id.person_username);
                txtRole = (TextView) view.findViewById(R.id.person_role);
                txtSubscribed = (TextView) view.findViewById(R.id.follower_subscribed_date);

                itemView.setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                if (mOnPersonSelectedListener != null) {
                    Person person = getPerson(getAdapterPosition());
                    mOnPersonSelectedListener.onPersonSelected(person);
                }
            }
        }
    }

    // Taken from http://stackoverflow.com/a/27037230
    private class PeopleItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        // use a custom drawable
        public PeopleItemDecoration(Context context, int resId) {
            mDivider = ContextCompat.getDrawable(context, resId);
        }

        @Override
        public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
            int left = parent.getPaddingLeft();
            int right = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i < childCount; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int top = child.getBottom() + params.bottomMargin;
                int bottom = top + mDivider.getIntrinsicHeight();

                mDivider.setBounds(left, top, right, bottom);
                mDivider.draw(c);
            }
        }
    }
}
