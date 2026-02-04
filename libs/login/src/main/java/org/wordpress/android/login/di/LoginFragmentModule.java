package org.wordpress.android.login.di;

import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment;
import org.wordpress.android.login.SignupMagicLinkFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginFragmentModule {
    @ContributesAndroidInjector
    abstract Login2FaFragment login2FaFragment();

    @ContributesAndroidInjector
    abstract LoginSiteAddressHelpDialogFragment loginSiteAddressHelpDialogFragment();

    @ContributesAndroidInjector
    abstract SignupMagicLinkFragment signupMagicLinkFragment();
}
