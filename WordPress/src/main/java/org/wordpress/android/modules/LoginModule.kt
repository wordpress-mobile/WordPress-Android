package org.wordpress.android.modules

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.wordpress.android.login.di.LoginFragmentModule

@InstallIn(SingletonComponent::class)
@Module(includes = [LoginFragmentModule::class])
interface LoginModule
