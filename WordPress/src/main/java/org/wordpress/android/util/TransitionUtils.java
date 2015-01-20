package org.wordpress.android.util;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Build;
import android.transition.AutoTransition;

public class TransitionUtils {

    // Set a lollipop or higher AutoTransition, otherwise use a fade
    public static void setFragmentTransition(FragmentTransaction transaction, Fragment fragment) {
        if (transaction == null || fragment == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            fragment.setEnterTransition(new AutoTransition());
        } else {
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        }
    }

}
