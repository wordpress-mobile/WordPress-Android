package org.wordpress.android.login.di;

import org.wordpress.android.login.LoginWpcomService;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class LoginServiceModule {
    @ContributesAndroidInjector
    abstract LoginWpcomService loginWpcomService();
}
