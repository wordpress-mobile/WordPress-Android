package org.wordpress.android.ui.people;

import android.app.Fragment;
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

    public static PeopleListFragment newInstance() {
        return new PeopleListFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.people_list_fragment, container, false);

        mListView = (ListView) rootView.findViewById(android.R.id.list);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //Person person = (Person) parent.getItemAtPosition(position);
                //TODO: show details for person
            }
        });

        return rootView;
    }

    public void setPeopleList(List<Person> peopleList) {
        PeopleAdapter peopleAdapter = (PeopleAdapter) mListView.getAdapter();
        if (peopleAdapter == null) {
            peopleAdapter = new PeopleAdapter(getActivity(), peopleList);
            mListView.setAdapter(peopleAdapter);
        } else {
            peopleAdapter.setPeopleList(peopleList);
            peopleAdapter.notifyDataSetChanged();
        }
    }
}
