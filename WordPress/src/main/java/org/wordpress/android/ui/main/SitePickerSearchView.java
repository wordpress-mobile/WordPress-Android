package org.wordpress.android.ui.main;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

public class SitePickerSearchView extends SearchView implements SearchView.OnQueryTextListener {
    private InputMethodManager mInputMethodManager;
    private SitePickerActivity mSitePickerActivity;

    public SitePickerSearchView(Context context) {
        super(context);
        mInputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public boolean onQueryTextSubmit(String s) {
        hideSoftKeyboard();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String s) {
        mSitePickerActivity.getAdapter().searchSites(s);
        return true;
    }

    public void configure(SitePickerActivity sitePickerActivity, MenuItem menuSearch) {
        mSitePickerActivity = sitePickerActivity;
        setIconifiedByDefault(false);
        setOnQueryTextListener(this);
        MenuItemCompat.setOnActionExpandListener(menuSearch, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                enableSearchMode();
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                disableSearchMode();
                return true;
            }
        });
    }

    public void enableSearchMode() {
        requestFocus();
        showSoftKeyboard();
        mSitePickerActivity.getAdapter().setIsInSearchMode(true);
        mSitePickerActivity.getAdapter().loadSites();
    }

    public void disableSearchMode() {
        hideSoftKeyboard();
        mSitePickerActivity.getAdapter().setIsInSearchMode(false);
        mSitePickerActivity.getAdapter().loadSites();
    }

    private void showSoftKeyboard() {
        if (!hasHardwareKeyboard()) {
            mInputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    public void hideSoftKeyboard() {
        if (!hasHardwareKeyboard()) {
            mInputMethodManager.hideSoftInputFromWindow(getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    private boolean hasHardwareKeyboard() {
        return (getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS);
    }
}
