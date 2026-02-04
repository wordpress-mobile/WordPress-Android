package org.wordpress.android.login.di;

import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginEmailFragment;
import org.wordpress.android.login.LoginGoogleFragment;
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment;
import org.wordpress.android.login.LoginUsernamePasswordFragment;
import org.wordpress.android.login.SignupConfirmationFragment;
import org.wordpress.android.login.SignupGoogleFragment;
import org.wordpress.android.login.SignupMagicLinkFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginFragmentModule {
    @ContributesAndroidInjector
    abstract Login2FaFragment login2FaFragment();

    @ContributesAndroidInjector
    abstract LoginEmailFragment loginEmailFragment();

    @ContributesAndroidInjector
    abstract LoginGoogleFragment loginGoogleFragment();

    @ContributesAndroidInjector
    abstract LoginSiteAddressHelpDialogFragment loginSiteAddressHelpDialogFragment();

    @ContributesAndroidInjector
    abstract LoginUsernamePasswordFragment loginUsernamePasswordFragment();

    @ContributesAndroidInjector
    abstract SignupGoogleFragment signupGoogleFragment();

    @ContributesAndroidInjector
    abstract SignupMagicLinkFragment signupMagicLinkFragment();

    @ContributesAndroidInjector
    abstract SignupConfirmationFragment signupConfirmationScreen();
}
