package org.wordpress.android.viewmodel;

import android.os.Bundle;
import android.support.annotation.NonNull;

interface ViewModelInterface {
    /**
     * ViewModels only survive configuration changes, so if the activity is completely destroyed and re-created, we will
     * need to re-create the ViewModel as well. Unfortunately, activity will not be aware of this and should call the
     * `onStart` method regardless of where it was a re-creation or a configuration change. We need a way to identify
     * such cases which is done through this method. A typical implementation would be to define a property called
     * `mIsStarted` and set that to true in `onStart` and return this variable from this method.
     * @return false if the ViewModel's `onStart` has been called before, return true otherwise.
     */
    boolean isStarted();

    /**
     * At the time of the construction of the ViewModel we may not have access to crucial information, such as the
     * current site because the ViewModels should be created through injection by ViewModelFactory. We need a different
     * way to do one time setups. This method should be implemented by every ViewModel to streamline that process.
     * It should first verify that the ViewModel has not been started yet by using the `isStarted()` method. An example
     * use case might be to start a fetch or refresh of the data the ViewModel will be using. The activity should be
     * calling this method in its `onCreate` and will not know if it's because of a configuration change or if it's
     * re-created. So, this method ensures that we do one time calls only when we have to.
     *
     * This method should be called after `readFromBundle` in the activity!
     */
    void onStart();

    /**
     * Although the ViewModel survives configuration changes, we'll often need some information to be able to re-create
     * the ViewModel from scratch. This method gives us a consistent way to do that. The activity should be calling this
     * method from its onCreate(Bundle savedInstanceState) method only if the `savedInstanceState` is not `null`.
     *
     * This method will be called when the activity is re-created AND after a configuration change. However, since we
     * should not be saving actual data in the bundle, it should be safe to override the property even if we already
     * have it set. If overriding a particular property feels wrong, it's most likely that the property doesn't belong
     * in the Bundle and should be handled in a different way.
     *
     * This method should be called before `onStart` in the activity!
     *
     * @param savedInstanceState should be passed from onCreate(Bundle savedInstanceState) when it's not `null`.
     */
    void readFromBundle(@NonNull Bundle savedInstanceState);

    /**
     * We need to save enough information to be able to re-create the ViewModel from scratch if the activity is
     * re-created and the cached data is lost. We should NOT save the actual data in the bundle. The bundle only meant
     * to hold small piece of information. A typical use case would be to save the `id` of an object or the `type` of
     * a list and read that in the `readFromBundle` method and ask the FluxC for the actual data. Since we shouldn't be
     * saving big amounts of data, it should be safe to write to the Bundle even during configuration changes since
     * it'll be a quick process.
     *
     * @param outState should be passed from the onSaveInstanceState(Bundle outState) of the activity.
     */
    void writeToBundle(@NonNull Bundle outState);
}
