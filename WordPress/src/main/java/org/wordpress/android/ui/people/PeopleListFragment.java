package org.wordpress.android.ui.people;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.models.Person;

import java.util.List;

public class PeopleListFragment extends Fragment {

    private ListView mListView;
    private OnPersonSelectedListener mListener;

    public static PeopleListFragment newInstance() {
        return new PeopleListFragment();
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_list_fragment, container, false);

        mListView = (ListView) rootView.findViewById(android.R.id.list);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Person person = (Person) parent.getItemAtPosition(position);
                mListener.onPersonSelected(person);
            }
        });

        return rootView;
    }

    public void setPeopleList(List<Person> peopleList) {
        if (!isAdded()) return;

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
