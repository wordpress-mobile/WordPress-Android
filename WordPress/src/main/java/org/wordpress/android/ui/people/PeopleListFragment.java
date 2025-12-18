package org.wordpress.android.ui.people;

import android.content.Context;
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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;

import org.apache.commons.text.StringEscapeUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.fluxc.model.RoleModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.models.JetpackPoweredScreen;
import org.wordpress.android.models.PeopleListFilter;
import org.wordpress.android.models.Person;
import org.wordpress.android.models.RoleUtils;
import org.wordpress.android.ui.ActionableEmptyView;
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.util.JetpackBrandingUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.WPAvatarUtils;
import org.wordpress.android.util.image.ImageManager;
import org.wordpress.android.util.image.ImageType;

import java.text.SimpleDateFormat;
import java.util.List;

import javax.inject.Inject;

public class PeopleListFragment extends Fragment {
    private SiteModel mSite;
    private OnPersonSelectedListener mOnPersonSelectedListener;
    private OnFetchPeopleListener mOnFetchPeopleListener;
    private ActionableEmptyView mActionableEmptyView;
    private RecyclerView mRecyclerView;
    private PeopleAdapter mPeopleAdapter;

    // previously this fragment enabled selecting a PeopleListFilter but this was changed to
    // show only users (TEAM) so we hard-code the filter here
    private static final PeopleListFilter PEOPLE_LIST_FILTER = PeopleListFilter.TEAM;

    @Inject SiteStore mSiteStore;
    @Inject ImageManager mImageManager;
    @Inject JetpackBrandingUtils mJetpackBrandingUtils;
    @Inject UiHelpers mUiHelpers;

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
    public void onDestroyView() {
        super.onDestroyView();
        mActionableEmptyView.button.setOnClickListener(null);
        // Reset adapter so it gets recreated with the new RecyclerView when the view is recreated
        mPeopleAdapter = null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnPersonSelectedListener = null;
        mOnFetchPeopleListener = null;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplicationContext()).component().inject(this);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
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
            actionBar.setTitle(R.string.users);
        }

        mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);

        mActionableEmptyView = rootView.findViewById(R.id.actionable_empty_view);

        mRecyclerView = rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.addItemDecoration(
                new DividerItemDecoration(mRecyclerView.getContext(), DividerItemDecoration.VERTICAL)
        );

        showJetpackBannerIfNeeded(rootView);

        return rootView;
    }

    private void showJetpackBannerIfNeeded(final View rootView) {
        if (mJetpackBrandingUtils.shouldShowJetpackBrandingForPhaseTwo()) {
            final JetpackPoweredScreen screen = JetpackPoweredScreen.WithStaticText.PERSON;
            View jetpackBannerView = rootView.findViewById(R.id.jetpack_banner);
            TextView jetpackBannerTextView = jetpackBannerView.findViewById(R.id.jetpack_banner_text);
            jetpackBannerTextView.setText(
                    mUiHelpers.getTextOfUiString(
                            requireContext(),
                            mJetpackBrandingUtils.getBrandingTextForScreen(screen))
            );
            RecyclerView scrollableView = mRecyclerView;

            mJetpackBrandingUtils.showJetpackBannerIfScrolledToTop(jetpackBannerView, scrollableView);
            mJetpackBrandingUtils.initJetpackBannerAnimation(jetpackBannerView, scrollableView);

            if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                jetpackBannerView.setOnClickListener(v -> {
                    mJetpackBrandingUtils.trackBannerTapped(screen);
                    new JetpackPoweredBottomSheetFragment()
                            .show(getChildFragmentManager(), JetpackPoweredBottomSheetFragment.TAG);
                });
            }
        }
    }

    @Override public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // important for accessibility - talkback
        getActivity().setTitle(R.string.users);
    }

    @Override
    public void onResume() {
        super.onResume();

        updatePeople(false);
    }

    private void updatePeople(boolean loadMore) {
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            showNetworkError();
            return;
        }

        if (mOnFetchPeopleListener != null) {
            if (loadMore) {
                mOnFetchPeopleListener.onFetchMorePeople(PEOPLE_LIST_FILTER);
            } else {
                boolean isFetching = mOnFetchPeopleListener.onFetchFirstPage(PEOPLE_LIST_FILTER);
                refreshPeopleList(isFetching);
            }
        }
    }

    private void showActionableEmptyView(
            int titleResId,
            int subtitleResId,
            int buttonTextResId,
            View.OnClickListener buttonClickListener
    ) {
        if (!isAdded()) {
            return;
        }
        mRecyclerView.setVisibility(View.GONE);
        mActionableEmptyView.title.setText(titleResId);
        if (subtitleResId != 0) {
            mActionableEmptyView.subtitle.setText(subtitleResId);
            mActionableEmptyView.subtitle.setVisibility(View.VISIBLE);
        } else {
            mActionableEmptyView.subtitle.setVisibility(View.GONE);
        }
        mActionableEmptyView.button.setText(buttonTextResId);
        mActionableEmptyView.button.setVisibility(View.VISIBLE);
        mActionableEmptyView.button.setOnClickListener(buttonClickListener);
        mActionableEmptyView.setVisibility(View.VISIBLE);
    }

    private void showNetworkError() {
        showActionableEmptyView(
                R.string.no_network_title,
                R.string.no_network_message,
                R.string.retry,
                v -> updatePeople(false)
        );
    }

    private void showFetchError() {
        showActionableEmptyView(
                R.string.error_fetch_users_list,
                0,
                R.string.retry,
                v -> updatePeople(false)
        );
    }

    private void showEmptyPeopleView() {
        showActionableEmptyView(
                R.string.people_empty_list_filtered_users,
                R.string.people_empty_list_filtered_users_subtitle,
                R.string.people_empty_list_filtered_users_button,
                v -> {
                    if (isAdded() && getActivity() instanceof PeopleManagementActivity) {
                        ((PeopleManagementActivity) getActivity()).inviteUser();
                    }
                }
        );
    }

    public void refreshPeopleList(boolean isFetching) {
        if (!isAdded()) {
            return;
        }

        List<Person> peopleList = PeopleTable.getUsers(mSite.getId());

        if (mPeopleAdapter == null) {
            mPeopleAdapter = new PeopleAdapter(requireActivity(), peopleList);
            mRecyclerView.setAdapter(mPeopleAdapter);
        } else {
            mPeopleAdapter.setPeopleList(peopleList);
        }

        if (!peopleList.isEmpty()) {
            // if the list is not empty, don't show any message
            mRecyclerView.setVisibility(View.VISIBLE);
            mActionableEmptyView.setVisibility(View.GONE);
        } else if (!isFetching) {
            // if we are not fetching and list is empty, show no content message
            showEmptyPeopleView();
        }
    }

    // Refresh the role display names after user roles is fetched
    public void refreshUserRoles() {
        if (mPeopleAdapter == null) {
            return;
        }
        mPeopleAdapter.refreshUserRoles();
        mPeopleAdapter.notifyDataSetChanged();
    }

    public void fetchingRequestFinished(PeopleListFilter filter, boolean isFirstPage, boolean isSuccessful) {
        if (PEOPLE_LIST_FILTER.equals(filter)) {
            if (isFirstPage) {
                if (isSuccessful) {
                    // Refresh the list with the newly fetched data
                    refreshPeopleList(false);
                } else {
                    showFetchError();
                }
            }
        }
    }

    // Container Activity must implement this interface
    public interface OnPersonSelectedListener {
        void onPersonSelected(Person person);
    }

    public interface OnFetchPeopleListener {
        boolean onFetchFirstPage(PeopleListFilter filter);

        void onFetchMorePeople(PeopleListFilter filter);
    }

    public class PeopleAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private final LayoutInflater mInflater;
        private final int mAvatarSz;
        private List<Person> mPeopleList;
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
                String avatarUrl = WPAvatarUtils.rewriteAvatarUrl(person.getAvatarUrl(), mAvatarSz);
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
                    Person person = getPerson(getBindingAdapterPosition());
                    mOnPersonSelectedListener.onPersonSelected(person);
                }
            }
        }
    }
}
