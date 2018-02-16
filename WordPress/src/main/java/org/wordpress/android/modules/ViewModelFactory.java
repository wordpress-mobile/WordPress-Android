package org.wordpress.android.modules;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
class ViewModelFactory implements ViewModelProvider.Factory {
    private final Map<Class<? extends ViewModel>, Provider<ViewModel>> mViewModelsMap;

    @Inject
    ViewModelFactory(Map<Class<? extends ViewModel>, Provider<ViewModel>> viewModelsMap) {
        this.mViewModelsMap = viewModelsMap;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> viewModelClass) {
        Provider<? extends ViewModel> creator = mViewModelsMap.get(viewModelClass);
        if (creator == null) {
            for (Map.Entry<Class<? extends ViewModel>, Provider<ViewModel>> entry : mViewModelsMap.entrySet()) {
                if (viewModelClass.isAssignableFrom(entry.getKey())) {
                    creator = entry.getValue();
                    break;
                }
            }
        }
        if (creator == null) {
            throw new IllegalArgumentException("View model not found [" + viewModelClass
                    + "]. Have you added corresponding method into the ViewModelModule.");
        }
        return (T) creator.get();
    }
}
