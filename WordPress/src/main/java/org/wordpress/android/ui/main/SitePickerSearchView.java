package org.wordpress.android.ui.main;

import android.content.Context;
import android.content.res.Configuration;
import android.support.v4.view.MenuItemCompat;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

public class SitePickerSearchView extends SearchView implements SearchView.OnQueryTextListener {
    private InputMethodManager mInputMethodManager;
    private SitePickerAdapter mSitePickerAdapter;

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
        if (mSitePickerAdapter != null) {
            mSitePickerAdapter.searchSites(s);
        }
        return true;
    }

    public void configure(MenuItem menuSearch) {
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

    public void setSitePickerAdapter(SitePickerAdapter sitePickerAdapter) {
        mSitePickerAdapter = sitePickerAdapter;
    }

    public void enableSearchMode() {
        requestFocus();
        showSoftKeyboard();
        if (mSitePickerAdapter != null) {
            mSitePickerAdapter.setIsInSearchMode(true);
            mSitePickerAdapter.loadSites();
        }
    }

    public void disableSearchMode() {
        hideSoftKeyboard();
        if (mSitePickerAdapter != null) {
            mSitePickerAdapter.setIsInSearchMode(false);
            mSitePickerAdapter.loadSites();
        }
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
