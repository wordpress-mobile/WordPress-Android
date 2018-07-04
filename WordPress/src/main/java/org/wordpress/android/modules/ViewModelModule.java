package org.wordpress.android.modules;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;

import org.wordpress.android.ui.pages.PageListViewModel;
import org.wordpress.android.ui.pages.PagesViewModel;
import org.wordpress.android.viewmodel.plugins.PluginBrowserViewModel;
import org.wordpress.android.viewmodel.ViewModelFactory;
import org.wordpress.android.viewmodel.ViewModelKey;
import org.wordpress.android.viewmodel.activitylog.ActivityLogDetailViewModel;
import org.wordpress.android.viewmodel.activitylog.ActivityLogViewModel;

import dagger.Binds;
import dagger.Module;
import dagger.multibindings.IntoMap;

@Module
abstract class ViewModelModule {
    @Binds
    @IntoMap
    @ViewModelKey(PluginBrowserViewModel.class)
    abstract ViewModel pluginBrowserViewModel(PluginBrowserViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ActivityLogViewModel.class)
    abstract ViewModel activityLogViewModel(ActivityLogViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(ActivityLogDetailViewModel.class)
    abstract ViewModel activityLogDetailViewModel(ActivityLogDetailViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PagesViewModel.class)
    abstract ViewModel pagesViewModel(PagesViewModel viewModel);

    @Binds
    @IntoMap
    @ViewModelKey(PageListViewModel.class)
    abstract ViewModel pageListViewModel(PageListViewModel viewModel);

    @Binds
    abstract ViewModelProvider.Factory provideViewModelFactory(ViewModelFactory viewModelFactory);
}
