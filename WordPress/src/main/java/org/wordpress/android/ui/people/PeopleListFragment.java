package org.wordpress.android.ui.people;

import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Person;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.WPNetworkImageView;

import java.util.List;

public class PeopleListFragment extends ListFragment implements OnItemClickListener {
    private static final String ARG_LOCAL_TABLE_BLOG_ID = "local_table_blog_id";

    private int mLocalTableBlogID;
    private OnPersonSelectedListener mOnPersonSelectedListener;
    private OnFetchMorePeopleListener mOnFetchMorePeopleListener;

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

        // progress bar that appears when loading more people
        mProgress = (ProgressBar) rootView.findViewById(R.id.progress_footer);
        mProgress.setVisibility(View.GONE);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLocalTableBlogID = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);
        getListView().setOnItemClickListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPeopleList();
    }

    public void refreshPeopleList() {
        if (!isAdded()) return;

        List<Person> peopleList = PeopleTable.getPeople(mLocalTableBlogID);

        PeopleAdapter peopleAdapter = (PeopleAdapter) getListAdapter();
        if (peopleAdapter == null) {
            peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
            setListAdapter(peopleAdapter);
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

    public class PeopleAdapter extends BaseAdapter {
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

        @Override
        public int getCount() {
            if (mPeopleList == null) {
                return 0;
            }
            return mPeopleList.size();
        }

        @Override
        public Person getItem(int position) {
            if (mPeopleList == null) {
                return null;
            }
            return mPeopleList.get(position);
        }

        @Override
        public long getItemId(int position) {
            Person person = getItem(position);
            if (person == null) {
                return -1;
            }
            return person.getPersonID();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final PeopleViewHolder holder;

            if (convertView == null || convertView.getTag() == null) {
                convertView = mInflater.inflate(R.layout.people_list_row, parent, false);
                holder = new PeopleViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (PeopleViewHolder) convertView.getTag();
            }

            Person person = getItem(position);

            if (person != null) {
                String avatarUrl = GravatarUtils.fixGravatarUrl(person.getAvatarUrl(), mAvatarSz);
                holder.imgAvatar.setImageUrl(avatarUrl, WPNetworkImageView.ImageType.AVATAR);
                holder.txtDisplayName.setText(person.getDisplayName());
                holder.txtUsername.setText(String.format("@%s", person.getUsername()));
                holder.txtRole.setText(StringUtils.capitalize(person.getRole()));
            }

            // end of list is reached
            if (mOnFetchMorePeopleListener != null && position == getCount() - 1) {
                mOnFetchMorePeopleListener.onFetchMorePeople();
            }

            return convertView;
        }

        private class PeopleViewHolder {
            private final WPNetworkImageView imgAvatar;
            private final TextView txtDisplayName;
            private final TextView txtUsername;
            private final TextView txtRole;

            PeopleViewHolder(View row) {
                imgAvatar = (WPNetworkImageView) row.findViewById(R.id.person_avatar);
                txtDisplayName = (TextView) row.findViewById(R.id.person_display_name);
                txtUsername = (TextView) row.findViewById(R.id.person_username);
                txtRole = (TextView) row.findViewById(R.id.person_role);
            }
        }
    }
}
