package org.wordpress.android.ui.people;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.datasets.PeopleTable;
import org.wordpress.android.models.Person;

import java.util.List;

public class PeopleListFragment extends Fragment {
    private static String ARG_LOCAL_TABLE_BLOG_ID = "LOCAL_TABLE_BLOG_ID";

    private int mLocalTableBlogID;
    private ListView mListView;
    private OnPersonSelectedListener mListener;

    public static PeopleListFragment newInstance(int localTableBlogID) {
        PeopleListFragment peopleListFragment = new PeopleListFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_LOCAL_TABLE_BLOG_ID, localTableBlogID);
        peopleListFragment.setArguments(bundle);
        return peopleListFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnPersonSelectedListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnPersonSelectedListener");
        }
    }

    // We need to override this for devices pre API 23
    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnPersonSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnPersonSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_list_fragment, container, false);

        mLocalTableBlogID = getArguments().getInt(ARG_LOCAL_TABLE_BLOG_ID);
        mListView = (ListView) rootView.findViewById(android.R.id.list);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mListener != null) {
                    Person person = (Person) parent.getItemAtPosition(position);
                    mListener.onPersonSelected(person);
                }
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshPeopleList();
    }

    public void refreshPeopleList() {
        if (!isAdded()) return;

        List<Person> peopleList = PeopleTable.getPeople(mLocalTableBlogID);

        PeopleAdapter peopleAdapter = (PeopleAdapter) mListView.getAdapter();
        if (peopleAdapter == null) {
            peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
            mListView.setAdapter(peopleAdapter);
        } else {
            peopleAdapter.setPeopleList(peopleList);
            peopleAdapter.notifyDataSetChanged();
        }
    }

    // Container Activity must implement this interface
    public interface OnPersonSelectedListener {
        void onPersonSelected(Person person);
    }
}
