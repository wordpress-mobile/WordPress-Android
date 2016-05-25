package org.wordpress.android.ui.people;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class PeopleListFragment extends Fragment implements OnItemClickListener {
    private static final String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";

    private int mLocalTableBlogID;
    private OnPersonSelectedListener mOnPersonSelectedListener;
    private OnFetchMorePeopleListener mOnFetchMorePeopleListener;

    private RecyclerView mRecyclerView;
    private ProgressBar mProgress;

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

    public void setOnFetchMorePeopleListener(OnFetchMorePeopleListener listener) {
        mOnFetchMorePeopleListener = listener;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mOnPersonSelectedListener = null;
        mOnFetchMorePeopleListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_list_fragment, container, false);

        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());

        // progress bar that appears when loading more people
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLocalTableBlogID = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);
        // set on item click listener
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPeopleList();
    }

    public void refreshPeopleList() {
        if (!isAdded()) return;

        List<Person> peopleList = PeopleTable.getPeople(mLocalTableBlogID);

        PeopleAdapter peopleAdapter = (PeopleAdapter) mRecyclerView.getAdapter();
        if (peopleAdapter == null) {
            peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
            mRecyclerView.setAdapter(peopleAdapter);
        } else {
            peopleAdapter.setPeopleList(peopleList);
        }
    }

    /*
    * show/hide progress bar which appears at the bottom of the activity when loading more people
    */
    public void showLoadingProgress(boolean showProgress) {
        if (isAdded() && mProgress != null) {
            if (showProgress) {
                mProgress.bringToFront();
                mProgress.setVisibility(View.VISIBLE);
            } else {
                mProgress.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mOnPersonSelectedListener != null) {
            Person person = (Person) parent.getItemAtPosition(position);
            mOnPersonSelectedListener.onPersonSelected(person);
        }
    }

    // Container Activity must implement this interface
    public interface OnPersonSelectedListener {
        void onPersonSelected(Person person);
    }

    public interface OnFetchMorePeopleListener {
        void onFetchMorePeople();
    }

    public class PeopleAdapter extends RecyclerView.Adapter<PeopleAdapter.PeopleViewHolder> {
        private final LayoutInflater mInflater;
        private List<Person> mPeopleList;
        private int mAvatarSz;

        public PeopleAdapter(Context context, List<Person> peopleList) {
            mAvatarSz = context.getResources().getDimensionPixelSize(R.dimen.people_avatar_sz);
            mInflater = LayoutInflater.from(context);
            mPeopleList = peopleList;
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
        public PeopleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = mInflater.inflate(R.layout.people_list_row, parent, false);

            return new PeopleViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PeopleAdapter.PeopleViewHolder holder, int position) {
            Person person = getPerson(position);

            if (person != null) {
                String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), mAvatarSz);
                holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                holder.txtDisplayName.setText(person.getDisplayName());
                holder.txtUsername.setText(String.format("@%s", person.getUsername()));
                holder.txtRole.setText(StringUtils.capitalize(person.getRole()));
            }

            // end of list is reached
            if (mOnFetchMorePeopleListener != null && position == getItemCount() - 1) {
                mOnFetchMorePeopleListener.onFetchMorePeople();
            }
        }

        public class PeopleViewHolder extends RecyclerView.ViewHolder {
            private final WPNetworkImageView imgAvatar;
            private final TextView txtDisplayName;
            private final TextView txtUsername;
            private final TextView txtRole;

            public PeopleViewHolder(View view) {
                super(view);
                imgAvatar = (WPNetworkImageView) view.findViewById(R.id.person_avatar);
                txtDisplayName = (TextView) view.findViewById(R.id.person_display_name);
                txtUsername = (TextView) view.findViewById(R.id.person_username);
                txtRole = (TextView) view.findViewById(R.id.person_role);
            }
        }
    }
}
