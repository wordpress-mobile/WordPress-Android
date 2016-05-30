package org.wordpress.android.ui.menus;

import android.app.Fragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wordpress.android.R;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.MenuLocationTable;
import org.wordpress.android.datasets.MenuTable;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.networking.menus.MenusRestWPCom;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.ui.menus.views.MenuAddEditRemoveView;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.List;

public class MenusFragment extends Fragment {

    private boolean mUndoPressed = false;
    private MenusRestWPCom mRestWPCom;
    private MenuAddEditRemoveView mAddEditRemoveControl;
    private boolean mRequestBeingProcessed;
    private int mCurrentRequestId;
    private boolean mIsUpdatingMenus;
    private TextView mEmptyView;
    private LinearLayout mSpinnersLayout;
    private MenusSpinner mMenuLocationsSpinner;
    private MenusSpinner mMenusSpinner;

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
            @Override public void onMenusReceived(int requestId, List<MenuModel> menus, List<MenuLocationModel> locations) {
                boolean bSpinnersUpdated = false;
                if (locations != null) {
                    if (CollectionUtils.areListsEqual(locations, mMenuLocationsSpinner.getItems())) {
                        // no op
                    } else {
                        // update Menu Locations spinner
                        mMenuLocationsSpinner.setItems((List)locations);
                        bSpinnersUpdated = true;
                    }
                }

                if (menus != null) {
                    if (CollectionUtils.areListsEqual(menus, mMenusSpinner.getItems())) {
                        // no op
                    } else {
                        // update Menus spinner
                        mMenusSpinner.setItems((List)menus);
                        bSpinnersUpdated = true;
                    }
                }

                if (bSpinnersUpdated) {
                    hideEmptyView();
                }
                mIsUpdatingMenus = false;
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
                if (mMenuLocationsSpinner.getCount() == 0 || mMenusSpinner.getCount() == 0) {
                    Toast.makeText(getActivity(), getString(R.string.could_not_load_menus), Toast.LENGTH_SHORT).show();
                    updateEmptyView(EmptyViewMessageType.NO_CONTENT);
                } else {
                    Toast.makeText(getActivity(), getString(R.string.could_not_refresh_menus), Toast.LENGTH_SHORT).show();
                }
                mRequestBeingProcessed = false;
                mIsUpdatingMenus = false;
            }
        });

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.menus_fragment, container, false);
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


        mMenuLocationsSpinner = (MenusSpinner) view.findViewById(R.id.menu_locations_spinner);
        mMenusSpinner = (MenusSpinner) view.findViewById(R.id.selected_menu_spinner);
        mEmptyView = (TextView) view.findViewById(R.id.empty_view);
        mSpinnersLayout = (LinearLayout) view.findViewById(R.id.spinner_group);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        updateMenus();
    }

    private void updateMenus() {
        if (mIsUpdatingMenus) {
            AppLog.w(AppLog.T.COMMENTS, "update comments task already running");
            return;
        }

        updateEmptyView(EmptyViewMessageType.LOADING);

        //immediately load/refresh whatever we have in our local db
        loadMenus();

        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            //we're offline
            return;
        }

        //also fetch latest menus from the server
        mIsUpdatingMenus = true;
        mCurrentRequestId = mRestWPCom.fetchAllMenus();

    }


    private void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView != null) {
            int stringId = 0;

            switch (emptyViewMessageType) {
                case LOADING:
                    stringId = R.string.loading;
                    break;
                case NO_CONTENT:
                    stringId = R.string.menus_spinner_empty;
                    break;
                case NETWORK_ERROR:
                    stringId = R.string.no_network_message;
                    break;
            }

            mEmptyView.setText(getText(stringId));
            mEmptyView.setVisibility(View.VISIBLE);

            if (mSpinnersLayout != null) {
                mSpinnersLayout.setVisibility(View.GONE);
            }
        }
    }

    private void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }

        if (mSpinnersLayout != null) {
            mSpinnersLayout.setVisibility(View.VISIBLE);
        }
    }


    /*
     * load menus using an AsyncTask
     */
    public void loadMenus() {
        if (mIsLoadTaskRunning) {
            AppLog.w(AppLog.T.MENUS, "load menus task already active");
        } else {
            new LoadMenusTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    /*
     * AsyncTask to load menus from SQLite
     */
    private boolean mIsLoadTaskRunning = false;
    private class LoadMenusTask extends AsyncTask<Void, Void, Boolean> {
        List<MenuModel> tmpMenus;
        List<MenuLocationModel> tmpMenuLocations;

        @Override
        protected void onPreExecute() {
            mIsLoadTaskRunning = true;
        }
        @Override
        protected void onCancelled() {
            mIsLoadTaskRunning = false;
        }
        @Override
        protected Boolean doInBackground(Void... params) {
            tmpMenus = MenuTable.getAllMenusForCurrentSite();
            tmpMenuLocations = MenuLocationTable.getAllMenuLocationsForCurrentSite();
            return true;
        }
        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                mMenuLocationsSpinner.setItems((List)tmpMenuLocations);
                mMenusSpinner.setItems((List)tmpMenus);
            }

            if ( (!result || tmpMenuLocations == null || tmpMenuLocations.size() == 0)
                    || tmpMenus == null || tmpMenus.size() == 0 ) {
                updateEmptyView(EmptyViewMessageType.NO_CONTENT);
            } else {
                hideEmptyView();
            }

            mIsLoadTaskRunning = false;
        }
    }


}

