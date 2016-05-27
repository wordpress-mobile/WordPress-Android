package org.wordpress.android.ui.menus;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuLocationTable;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.models.NameInterface;
import org.wordpress.android.networking.menus.MenusRestWPCom;
import org.wordpress.android.ui.menus.views.MenuAddEditRemoveView;

import java.util.ArrayList;
import java.util.List;

public class MenusFragment extends Fragment {

    private boolean mUndoPressed = false;
    private MenusRestWPCom mRestWPCom;
    private MenuAddEditRemoveView mAddEditRemoveControl;
    private List<MenuModel> mMenus = new ArrayList<>();
    private TextView mAllMenuNamesTextView;
    private boolean mRequestBeingProcessed;
    private int mCurrentRequestId;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mRestWPCom = new MenusRestWPCom(new MenusRestWPCom.MenusListener() {
            @Override public long getSiteId() {
                return Long.valueOf(WordPress.getCurrentRemoteBlogId());
            }
            @Override public void onMenuCreated(int requestId, MenuModel menu) {
                Toast.makeText(getActivity(), "menu: " + menu.name + " created", Toast.LENGTH_SHORT).show();
                mRequestBeingProcessed = false;
            }
            @Override public Context getContext() { return getActivity(); }
            @Override public void onMenusReceived(int requestId, List<MenuModel> menus) {
                mMenus = menus;
                if (mMenus != null) {

                    if (mMenus.size() > 0){
                        Toast.makeText(getActivity(), "menus successfully retrieved", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "menus came empty", Toast.LENGTH_SHORT).show();
                    }

                    if (mMenus.size() == 1){
                        mAddEditRemoveControl.setMenu(mMenus.get(0), false);
                    } else {
                        String menuNames = "";
                        for (MenuModel menu : mMenus){
                            if (menu.name != null){
                                menuNames = menuNames + menu.name + "\n";
                            }
                        }
                        mAllMenuNamesTextView.setText(menuNames);
                    }
                }
                mRequestBeingProcessed = false;
            }

            @Override public void onMenuDeleted(int requestId, MenuModel menu, boolean deleted) {
                if (deleted)
                    Toast.makeText(getActivity(), "menu: " + menu.name + " deleted", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "menu: " + menu.name + " delete request NOT DELETED", Toast.LENGTH_SHORT).show();

                mRequestBeingProcessed = false;
            }
            @Override public void onMenuUpdated(int requestId, MenuModel menu) {
                Toast.makeText(getActivity(), "menu: " + menu.name + " updated", Toast.LENGTH_SHORT).show();
                mRequestBeingProcessed = false;
            }

            @Override
            public void onErrorResponse(int requestId, MenusRestWPCom.REST_ERROR error) {
                Toast.makeText(getActivity(), "could not retrieve menus", Toast.LENGTH_SHORT).show();
                mRequestBeingProcessed = false;
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.menus_fragment, container, false);
        mAllMenuNamesTextView = (TextView) view.findViewById(R.id.test_text);
        mAddEditRemoveControl = (MenuAddEditRemoveView) view.findViewById(R.id.menu_add_edit_remove_view);
        mAddEditRemoveControl.setMenuActionListener(new MenuAddEditRemoveView.MenuAddEditRemoveActionListener() {
            @Override
            public void onMenuCreate(MenuModel menu) {
                mRestWPCom.createMenu(menu);
            }

            @Override
            public void onMenuDelete(final MenuModel menu) {

                View.OnClickListener undoListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mUndoPressed = true;
                        // user undid the trash action, so reset the control to whatever it had
                        mAddEditRemoveControl.setMenu(menu, false);
                    }
                };

                Snackbar snackbar = Snackbar.make(getView(), getString(R.string.menus_menu_deleted), Snackbar.LENGTH_LONG)
                        .setAction(R.string.undo, undoListener);

                // wait for the undo snackbar to disappear before actually deleting the post
                snackbar.setCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        super.onDismissed(snackbar, event);
                        if (mUndoPressed) {
                            mUndoPressed = false;
                            return;
                        }

                        if (!mRequestBeingProcessed) {
                            mRequestBeingProcessed = true;
                            mCurrentRequestId = mRestWPCom.deleteMenu(menu);
                        }
                    }
                });

                snackbar.show();
            }

            @Override
            public void onMenuUpdate(MenuModel menu) {
                if (!mRequestBeingProcessed) {
                    mRequestBeingProcessed = true;
                    mCurrentRequestId = mRestWPCom.updateMenu(menu);
                }
            }
        });


        MenusSpinner menuLocationsSpinner = (MenusSpinner) view.findViewById(R.id.menu_locations_spinner);
        MenusSpinner selectedMenuSpinner = (MenusSpinner) view.findViewById(R.id.selected_menu_spinner);
//        menuLocationsSpinner.setItems(new String[]{"Primary Menu", "Social Links"});
//        selectedMenuSpinner.setItems(new String[]{"Main Menu", "Social Menu", "Professional Menu", "Test Menu", "New Menu"});
        List menuLocations  = MenuLocationTable.getAllMenuLocationsForCurrentSite();
        if (menuLocations == null) {
            //TODO show an error dialog here and dismiss this activity as we can't possibly show the user anything else
        } else {
            menuLocationsSpinner.setItems(menuLocations);
        }



        //FIXME: start - delete all this test code!
        Button setMenuBtn = (Button) view.findViewById(R.id.menu_test_set_menu);
        setMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuModel model = new MenuModel();
                model.name = "test menu name";
                mAddEditRemoveControl.setMenu(model, false);
            }
        });

        Button setDefaultMenuBtn = (Button) view.findViewById(R.id.menu_test_set_menu_default);
        setDefaultMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MenuModel model = new MenuModel();
                model.name = "Default menu can't be trashed";
                mAddEditRemoveControl.setMenu(model, true);
            }
        });

        Button resetMenuBtn = (Button) view.findViewById(R.id.menu_test_reset_control);
        resetMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAddEditRemoveControl.setMenu(null, false);
                mAllMenuNamesTextView.setText(null);
            }
        });


        // fetching buttons

        Button fetchAllMenusBtn = (Button) view.findViewById(R.id.menu_test_fetch_all);
        fetchAllMenusBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRequestBeingProcessed) {
                    mRequestBeingProcessed = true;
                    mCurrentRequestId = mRestWPCom.fetchAllMenus();
                }
            }
        });

        Button fetchGoodMenuBtn = (Button) view.findViewById(R.id.menu_test_fetch_good_menu);
        fetchGoodMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMenus.size() > 0) {
                    if (!mRequestBeingProcessed) {
                        mRequestBeingProcessed = true;
                        mCurrentRequestId = mRestWPCom.fetchMenu(mMenus.get(mMenus.size() - 1).menuId);
                    }
                } else {
                    Toast.makeText(getActivity(), "Please fetch all menus first", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button fetchInexistentMenuBtn = (Button) view.findViewById(R.id.menu_test_fetch_bad_menu);
        fetchInexistentMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRequestBeingProcessed) {
                    mRequestBeingProcessed = true;
                    mCurrentRequestId = mRestWPCom.fetchMenu(4836); //fake number
                }
            }
        });

        Button fetchZeroIdMenuBtn = (Button) view.findViewById(R.id.menu_test_fetch_zero_menu);
        fetchZeroIdMenuBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRequestBeingProcessed) {
                    mRequestBeingProcessed = true;
                    mCurrentRequestId = mRestWPCom.fetchMenu(0);
                }
            }
        });

        //FIXME: end - delete all this test code!

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
