package org.wordpress.android.modules;

import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProvider;
import android.support.annotation.NonNull;
import android.support.v4.util.ArrayMap;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ViewModelFactory implements ViewModelProvider.Factory {
    private final ArrayMap<Class<? extends ViewModel>, Provider<ViewModel>> mViewModelsMap;

    @Inject
    ViewModelFactory(ArrayMap<Class<? extends ViewModel>, Provider<ViewModel>> viewModelsMap) {
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
                    + "]. Have you registered the viewModel in the ViewModelModule.provideViewModelFactory() "
                    + "method? ");
        }
        return (T) creator.get();
    }
}
