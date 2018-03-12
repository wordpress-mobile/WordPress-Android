package org.wordpress.android.login.di;

import org.wordpress.android.login.Login2FaFragment;
import org.wordpress.android.login.LoginEmailFragment;
import org.wordpress.android.login.LoginEmailPasswordFragment;
import org.wordpress.android.login.LoginGoogleFragment;
import org.wordpress.android.login.LoginMagicLinkRequestFragment;
import org.wordpress.android.login.LoginMagicLinkSentFragment;
import org.wordpress.android.login.LoginSiteAddressFragment;
import org.wordpress.android.login.LoginSiteAddressHelpDialogFragment;
import org.wordpress.android.login.LoginUsernamePasswordFragment;
import org.wordpress.android.login.SignupEmailFragment;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginFragmentModule {
    @ContributesAndroidInjector
    abstract Login2FaFragment login2FaFragment();

    @ContributesAndroidInjector
    abstract LoginEmailFragment loginEmailFragment();

    @ContributesAndroidInjector
    abstract LoginEmailPasswordFragment loginEmailPasswordFragment();

    @ContributesAndroidInjector
    abstract LoginGoogleFragment loginGoogleFragment();

    @ContributesAndroidInjector
    abstract LoginMagicLinkRequestFragment loginMagicLinkRequestFragment();

    @ContributesAndroidInjector
    abstract LoginMagicLinkSentFragment loginMagicLinkSentFragment();

    @ContributesAndroidInjector
    abstract LoginSiteAddressFragment loginSiteAddressFragment();

    @ContributesAndroidInjector
    abstract LoginSiteAddressHelpDialogFragment loginSiteAddressHelpDialogFragment();

    @ContributesAndroidInjector
    abstract LoginUsernamePasswordFragment loginUsernamePasswordFragment();

    @ContributesAndroidInjector
    abstract SignupEmailFragment signupEmailFragment();
}
