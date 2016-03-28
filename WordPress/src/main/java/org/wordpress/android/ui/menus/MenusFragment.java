package org.wordpress.android.ui.menus;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;

public class MenusFragment extends Fragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        View view = inflater.inflate(R.layout.menus_fragment, container, false);

        MenusSpinner menuLocationsSpinner = (MenusSpinner) view.findViewById(R.id.menu_locations_spinner);
        MenusSpinner selectedMenuSpinner = (MenusSpinner) view.findViewById(R.id.selected_menu_spinner);
        menuLocationsSpinner.setItems(new String[]{"Primary Menu", "Social Links"});
        selectedMenuSpinner.setItems(new String[]{"Main Menu", "Social Menu", "Professional Menu", "Test Menu", "New Menu"});

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }
}
