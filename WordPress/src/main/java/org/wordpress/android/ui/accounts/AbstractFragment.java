package org.wordpress.android.ui.accounts;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;

import org.wordpress.android.util.ActivityUtils;

import java.util.Locale;

/**
 * A fragment representing a single step in a wizard. The fragment shows a dummy title indicating
 * the page number, along with some dummy text.
 */
public abstract class AbstractFragment extends Fragment {
    protected ConnectivityManager mSystemService;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSystemService = (ConnectivityManager) getActivity().getApplicationContext().
                getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    protected void startProgress(String message) {
    }

    protected void endProgress() {
    }

    protected abstract void onDoneAction();

    protected abstract boolean isUserDataValid();

    protected boolean onDoneEvent(int actionId, KeyEvent event) {
        if (didPressEnterKey(actionId, event)) {
            if (!isUserDataValid()) {
                return true;
            }

            // hide keyboard before calling the done action
            if (getActivity() != null) {
                ActivityUtils.hideKeyboardForced(getActivity().getCurrentFocus());
            }

            // call child action
            onDoneAction();
            return true;
        }
        return false;
    }

    protected boolean didPressEnterKey(int actionId, KeyEvent event) {
        return actionId == EditorInfo.IME_ACTION_DONE || event != null && (event.getAction() == KeyEvent.ACTION_DOWN
                                                                           && event.getKeyCode()
                                                                              == KeyEvent.KEYCODE_ENTER);
    }

    protected void lowerCaseEditable(Editable editable) {
        // Convert editable content to lowercase
        String lowerCase = editable.toString().toLowerCase(Locale.ROOT);
        if (!lowerCase.equals(editable.toString())) {
            editable.replace(0, editable.length(), lowerCase);
        }
    }
}
