package org.wordpress.android.modules;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.v4.util.ArrayMap;

import org.wordpress.android.viewmodel.PluginBrowserViewModel;

import javax.inject.Provider;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(subcomponents = ViewModelSubComponent.class)
abstract class ViewModelModule {
    @Provides
    @Singleton
    static ViewModelProvider.Factory provideViewModelFactory(ViewModelSubComponent.Builder subComponentBuilder) {
        ArrayMap<Class<? extends ViewModel>, Provider<ViewModel>> viewModelMap = new ArrayMap<>();
        final ViewModelSubComponent subComp = subComponentBuilder.build();
        viewModelMap.put(PluginBrowserViewModel.class, new Provider<ViewModel>() {
            @Override
            public ViewModel get() {
                return subComp.pluginBrowserViewModel();
            }
        });
        return new ViewModelFactory(viewModelMap);
    }
}
