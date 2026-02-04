package org.wordpress.android.login.di;

import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment;
import org.wordpress.android.login.SignupMagicLinkFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginFragmentModule {
    @ContributesAndroidInjector
    abstract LoginSiteAddressHelpDialogFragment loginSiteAddressHelpDialogFragment();

    @ContributesAndroidInjector
    abstract SignupMagicLinkFragment signupMagicLinkFragment();
}
