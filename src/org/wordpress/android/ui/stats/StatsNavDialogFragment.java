package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

import org.wordpress.android.R;

/**
 * A fragment to display the list of stats view types for the user to navigate to.
 * Only visible on phone.
 */
public class StatsNavDialogFragment extends SherlockDialogFragment implements OnItemClickListener {

    private static final String ARGS_POSITION = "ARGS_POSITION";
    
    public static final String TAG = StatsNavDialogFragment.class.getSimpleName();

    public interface NavigationListener {
        public void onItemClick(int position);
    }
    
    private NavigationListener mListener;
    private int mPosition = 0;
    private CustomAdapter mAdapter;

    public static StatsNavDialogFragment newInstance(int mNavPosition) {

        StatsNavDialogFragment fragment = new StatsNavDialogFragment();
        
        Bundle bundle = new Bundle();
        bundle.putInt(ARGS_POSITION, mNavPosition);
        
        fragment.setArguments(bundle);
        return fragment;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            mListener = (NavigationListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement NavigationListener");
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable());
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().setCanceledOnTouchOutside(true);
        
        View view = inflater.inflate(R.layout.stats_nav_list, container, false);
        ListView lv = (ListView) view.findViewById(R.id.stats_nav_listview);  
        mAdapter = new CustomAdapter(StatsViewType.toStringArray());
        lv.setAdapter(mAdapter);
        lv.setOnItemClickListener(this);
        
        mPosition = getArgsPosition();
        lv.setSelection(mPosition);
        
        return view;
    }

    private int getArgsPosition() {
        return getArguments().getInt(ARGS_POSITION);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        mPosition  = position;
        mAdapter.notifyDataSetChanged();
        mListener.onItemClick(position);
    }
    
    private class CustomAdapter extends BaseAdapter {

        private String[] mStrings;

        public CustomAdapter(String[] objects) {
            mStrings = objects;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(parent.getContext());
                convertView = inflater.inflate(R.layout.stats_nav_cell, parent, false);
            }
            
            TextView tv = (TextView) convertView.findViewById(R.id.stats_nav_cell_text);
            tv.setText((CharSequence) getItem(position));
            
            ImageView checkmark = (ImageView) convertView.findViewById(R.id.stats_nav_cell_checkmark);
            if (position == mPosition)
                checkmark.setVisibility(View.VISIBLE);
            else
                checkmark.setVisibility(View.INVISIBLE);
            
            return convertView;
        }

        @Override
        public int getCount() {
            return mStrings.length;
        }

        @Override
        public Object getItem(int position) {
            return mStrings[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
        
    }

}
