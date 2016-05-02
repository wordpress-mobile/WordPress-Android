package org.wordpress.android.ui.menus;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.ui.menus.views.MenuAddEditRemoveView;

public class MenusFragment extends Fragment {
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.menus_fragment, container, false);

        final MenuAddEditRemoveView control = (MenuAddEditRemoveView) view.findViewById(R.id.menu_add_edit_remove_view);
        control.setMenuActionListener(new MenuAddEditRemoveView.MenuAddEditRemoveActionListener() {
            @Override
            public void onMenuCreate(MenuModel menu) {
                // TODO implement menu created listener
                Toast.makeText(getActivity(), "menu: " + menu.name + " create request",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuDelete(MenuModel menu) {
                // TODO implement menu deleted listener
                Toast.makeText(getActivity(), "menu: " + menu.name + " delete request",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onMenuUpdate(MenuModel menu) {
                // TODO implement menu updated listener
                Toast.makeText(getActivity(), "menu: " + menu.name + " update request",Toast.LENGTH_SHORT).show();
            }
        });

        //FIXME: start - delete all this!
        Button setMenuBtn = (Button) view.findViewById(R.id.menu_test_set_menu);
        setMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuModel model = new MenuModel();
                model.name = "test menu name";
                control.setMenu(model, false);
            }
        });

        Button setDefaultMenuBtn = (Button) view.findViewById(R.id.menu_test_set_menu_default);
        setDefaultMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuModel model = new MenuModel();
                model.name = "Default menu can't be trashed";
                control.setMenu(model, true);
            }
        });

        Button resetMenuBtn = (Button) view.findViewById(R.id.menu_test_reset_control);
        resetMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                control.setMenu(null, false);
            }
        });
        //FIXME: end - delete all this!

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
