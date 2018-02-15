package org.wordpress.android.modules;

import org.wordpress.android.viewmodel.PluginBrowserViewModel;

import dagger.Subcomponent;

@Subcomponent
public interface ViewModelSubComponent {

    PluginBrowserViewModel pluginBrowserViewModel();

    @Subcomponent.Builder
    interface Builder {
        ViewModelSubComponent build();
    }
}
