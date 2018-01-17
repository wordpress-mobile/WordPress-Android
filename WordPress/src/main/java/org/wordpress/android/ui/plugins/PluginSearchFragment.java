package org.wordpress.android.ui.plugins;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel;

import java.util.List;

public class PluginSearchFragment extends Fragment {

    public static final String TAG = PluginSearchFragment.class.getName();

    public static PluginSearchFragment newInstance() {
        PluginSearchFragment fragment = new PluginSearchFragment();
        Bundle bundle = new Bundle();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.plugin_search_fragment, container, false);
        return view;
    }

    public void setSearchResults(@NonNull List<WPOrgPluginModel> plugins) {
        // TODO
    }
}