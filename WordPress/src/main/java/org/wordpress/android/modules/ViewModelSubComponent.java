package org.wordpress.android.modules;

import org.wordpress.android.ui.plugins.PluginBrowserViewModel;
import org.wordpress.android.ui.stats.models.PostViewsModel;

import dagger.Subcomponent;

@Subcomponent
public interface ViewModelSubComponent {

    PluginBrowserViewModel pluginBrowserVM();

    @Subcomponent.Builder
    interface Builder {
        ViewModelSubComponent build();
    }
}
