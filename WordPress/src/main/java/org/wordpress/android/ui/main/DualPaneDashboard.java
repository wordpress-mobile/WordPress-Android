package org.wordpress.android.ui.main;

import android.os.Bundle;

public interface DualPaneDashboard {

    void addContentFragment(Class contentFragmentClass, Bundle parameters);

    void removeContentFragment();

    boolean isFragmentAdded(Class contentFragmentClass);
}
