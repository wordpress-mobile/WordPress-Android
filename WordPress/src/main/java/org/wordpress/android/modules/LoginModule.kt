package org.wordpress.android.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.login.di.LoginFragmentModule
import org.wordpress.android.login.di.LoginServiceModule

@InstallIn(SingletonComponent::class)
@Module(includes = [LoginFragmentModule::class, LoginServiceModule::class])
interface LoginModule
