package org.wordpress.android.ui.people;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import com.google.android.material.appbar.AppBarLayout;

import org.apache.commons.text.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.FilteredRecyclerView;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.RtlUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class PeopleListFragment extends Fragment {
    private SiteModel mSite;
    private OnPersonSelectedListener mOnPersonSelectedListener;
    private OnFetchPeopleListener mOnFetchPeopleListener;
    private ActionableEmptyView mActionableEmptyView;
    private FilteredRecyclerView mFilteredRecyclerView;
    private PeopleListFilter mPeopleListFilter;

    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;

    public static PeopleListFragment newInstance(SiteModel site) {
        PeopleListFragment peopleListFragment = new PeopleListFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(WordPress.SITE, site);
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
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

        Toolbar toolbar = rootView.findViewById(R.id.toolbar_main);
        ((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.people);
        }

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
        final boolean isPrivate = mSite != null && mSite.isPrivate();

        mActionableEmptyView = rootView.findViewById(R.id.actionable_empty_view);
        mFilteredRecyclerView = rootView.findViewById(R.id.filtered_recycler_view);
        mFilteredRecyclerView
                .addItemDecoration(new PeopleItemDecoration(getActivity()));
        mFilteredRecyclerView.setLogT(AppLog.T.PEOPLE);
        mFilteredRecyclerView.setSwipeToRefreshEnabled(false);

        // the following will change the look and feel of the toolbar to match the current design
        mFilteredRecyclerView.setToolbarLeftAndRightPadding(
                getResources().getDimensionPixelSize(R.dimen.margin_filter_spinner),
                getResources().getDimensionPixelSize(R.dimen.margin_none));

        mFilteredRecyclerView.setFilterListener(new FilteredRecyclerView.FilterListener() {
            @Override
            public List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh) {
                ArrayList<FilterCriteria> list = new ArrayList<>();
                Collections.addAll(list, PeopleListFilter.values());
                // Only a private blog can have viewers
                if (!isPrivate) {
                    list.remove(PeopleListFilter.VIEWERS);
                }
                return list;
            }

            @Override
            public void onLoadFilterCriteriaOptionsAsync(
                    FilteredRecyclerView.FilterCriteriaAsyncLoaderListener listener, boolean refresh) {
                // no-op
            }

            @Override
            public FilterCriteria onRecallSelection() {
                mPeopleListFilter = AppPrefs.getPeopleListFilter();

                // if viewers is not available for this blog, set the filter to TEAM
                if (mPeopleListFilter == PeopleListFilter.VIEWERS && !isPrivate) {
                    mPeopleListFilter = PeopleListFilter.TEAM;
                    AppPrefs.setPeopleListFilter(mPeopleListFilter);
                }
                return mPeopleListFilter;
            }

            @Override
            public void onLoadData(boolean forced) {
                updatePeople(false);
            }

            @Override
            public void onFilterSelected(int position, FilterCriteria criteria) {
                mPeopleListFilter = (PeopleListFilter) criteria;
                AppPrefs.setPeopleListFilter(mPeopleListFilter);
            }

            @Override
            public String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType) {
                mActionableEmptyView.setVisibility(View.GONE);
                mFilteredRecyclerView.setToolbarScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                                            | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);

                switch (emptyViewMsgType) {
                    case LOADING:
                        return getString(R.string.people_fetching);
                    case NETWORK_ERROR:
                        return getString(R.string.no_network_message);
                    case NO_CONTENT:
                        String title = "";

                        switch (mPeopleListFilter) {
                            case TEAM:
                                title = getString(R.string.people_empty_list_filtered_users);
                                break;
                            case FOLLOWERS:
                                title = getString(R.string.people_empty_list_filtered_followers);
                                break;
                            case EMAIL_FOLLOWERS:
                                title = getString(R.string.people_empty_list_filtered_email_followers);
                                break;
                            case VIEWERS:
                                title = getString(R.string.people_empty_list_filtered_viewers);
                                break;
                        }

                        mActionableEmptyView.title.setText(title);
                        mActionableEmptyView.setVisibility(View.VISIBLE);
                        mFilteredRecyclerView.setToolbarScrollFlags(0);
                        return "";
                    case GENERIC_ERROR:
                        switch (mPeopleListFilter) {
                            case TEAM:
                                return getString(R.string.error_fetch_users_list);
                            case FOLLOWERS:
                                return getString(R.string.error_fetch_followers_list);
                            case EMAIL_FOLLOWERS:
                                return getString(R.string.error_fetch_email_followers_list);
                            case VIEWERS:
                                return getString(R.string.error_fetch_viewers_list);
                        }
                    default:
                        return "";
                }
            }

            @Override
            public void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType) {
            }
        });

        return rootView;
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.people);
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
        if (!isAdded()) {
            return;
        }

        List<Person> peopleList;
        switch (mPeopleListFilter) {
            case TEAM:
                peopleList = PeopleTable.getUsers(mSite.getId());
                break;
            case FOLLOWERS:
                peopleList = PeopleTable.getFollowers(mSite.getId());
                break;
            case EMAIL_FOLLOWERS:
                peopleList = PeopleTable.getEmailFollowers(mSite.getId());
                break;
            case VIEWERS:
                peopleList = PeopleTable.getViewers(mSite.getId());
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
            mFilteredRecyclerView.setToolbarScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                                        | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
            mActionableEmptyView.setVisibility(View.GONE);
        } else if (!isFetching) {
            // if we are not fetching and list is empty, show no content message
            mFilteredRecyclerView.updateEmptyView(EmptyViewMessageType.NO_CONTENT);
        }
    }

    // Refresh the role display names after user roles is fetched
    public void refreshUserRoles() {
        if (mFilteredRecyclerView == null) {
            // bail when list is not available
            return;
        }

        PeopleAdapter peopleAdapter = (PeopleAdapter) mFilteredRecyclerView.getAdapter();
        if (peopleAdapter != null) {
            peopleAdapter.refreshUserRoles();
            peopleAdapter.notifyDataSetChanged();
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
        private List<RoleModel> mUserRoles;

        public PeopleAdapter(Context context, List<Person> peopleList) {
            mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
            mInflater = LayoutInflater.from(context);
            mPeopleList = peopleList;
            setHasStableIds(true);
            refreshUserRoles();
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

        public void refreshUserRoles() {
            if (mSite != null) {
                mUserRoles = mSiteStore.getUserRoles(mSite);
            }
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

        @NonNull
        @Override
        public PeopleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.people_list_row, parent, false);

            return new PeopleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            PeopleViewHolder peopleViewHolder = (PeopleViewHolder) holder;
            final Person person = getPerson(position);

            if (person != null) {
                String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), mAvatarSz);
                mImageManager.loadIntoCircle(peopleViewHolder.mImgAvatar, ImageType.AVATAR_WITH_BACKGROUND, avatarUrl);
                peopleViewHolder.mTxtDisplayName.setText(StringEscapeUtils.unescapeHtml4(person.getDisplayName()));
                if (person.getRole() != null) {
                    peopleViewHolder.mTxtRole.setVisibility(View.VISIBLE);
                    peopleViewHolder.mTxtRole.setText(RoleUtils.getDisplayName(person.getRole(), mUserRoles));
                } else {
                    peopleViewHolder.mTxtRole.setVisibility(View.GONE);
                }
                if (!person.getUsername().isEmpty()) {
                    peopleViewHolder.mTxtUsername.setVisibility(View.VISIBLE);
                    peopleViewHolder.mTxtUsername.setText(String.format("@%s", person.getUsername()));
                } else {
                    peopleViewHolder.mTxtUsername.setVisibility(View.GONE);
                }
                if (person.getPersonType() == Person.PersonType.USER
                    || person.getPersonType() == Person.PersonType.VIEWER) {
                    peopleViewHolder.mTxtSubscribed.setVisibility(View.GONE);
                } else {
                    peopleViewHolder.mTxtSubscribed.setVisibility(View.VISIBLE);
                    String dateSubscribed = SimpleDateFormat.getDateInstance().format(person.getDateSubscribed());
                    String dateText = getString(R.string.follower_subscribed_since, dateSubscribed);
                    peopleViewHolder.mTxtSubscribed.setText(dateText);
                }
            }

            // end of list is reached
            if (position == getItemCount() - 1) {
                updatePeople(true);
            }
        }

        @Override public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            PeopleViewHolder peopleViewHolder = (PeopleViewHolder) holder;
        }

        public class PeopleViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            private final ImageView mImgAvatar;
            private final TextView mTxtDisplayName;
            private final TextView mTxtUsername;
            private final TextView mTxtRole;
            private final TextView mTxtSubscribed;

            public PeopleViewHolder(View view) {
                super(view);
                mImgAvatar = view.findViewById(R.id.person_avatar);
                mTxtDisplayName = view.findViewById(R.id.person_display_name);
                mTxtUsername = view.findViewById(R.id.person_username);
                mTxtRole = view.findViewById(R.id.person_role);
                mTxtSubscribed = view.findViewById(R.id.follower_subscribed_date);

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
        private InsetDrawable mDivider;

        // use a custom drawable
        PeopleItemDecoration(Context context) {
            int[] attrs = {android.R.attr.listDivider};
            TypedArray ta = context.obtainStyledAttributes(attrs);
            Drawable drawable = ta.getDrawable(0);
            ta.recycle();

            int inset = context.getResources().getDimensionPixelOffset(R.dimen.people_list_divider_left_margin);

            if (RtlUtils.isRtl(context)) {
                mDivider = new InsetDrawable(drawable, 0, 0, inset, 0);
            } else {
                mDivider = new InsetDrawable(drawable, inset, 0, 0, 0);
            }
        }

        @Override
        public void onDraw(@NotNull Canvas c, @NotNull RecyclerView parent, @NotNull RecyclerView.State state) {
            int left = ViewCompat.getPaddingStart(parent);
            int right = parent.getWidth() - ViewCompat.getPaddingEnd(parent);

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
