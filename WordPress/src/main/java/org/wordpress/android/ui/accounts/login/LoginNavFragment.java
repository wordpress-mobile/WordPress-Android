package org.wordpress.android.ui.accounts.login;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import org.wordpress.android.ui.accounts.login.nav.LoginStateGetter;
import org.wordpress.android.ui.accounts.login.nav.LoginNav;
import org.wordpress.android.ui.accounts.login.nav.LoginNavController;
import org.wordpress.android.ui.accounts.login.nav.LoginNavHandler;

public class LoginNavFragment extends Fragment implements LoginStateGetter.FsmGetter {
    private LoginNavController mLoginNavController;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // retain this fragment as it serves as a headless login nav state keeper
        setRetainInstance(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LoginNavHandler) {
            if (mLoginNavController == null) {
                mLoginNavController = new LoginNavController(LoginNav.Prologue.class);
            }

            mLoginNavController.setLoginNavHandler((LoginNavHandler) context);
        } else {
            throw new RuntimeException(context.toString() + " must implement LoginNavHandler");
        }
    }

    @Override
    public LoginStateGetter getLoginStateGetter() {
        return mLoginNavController;
    }

    public void goBack() {
        mLoginNavController.goBack();
    }
}
