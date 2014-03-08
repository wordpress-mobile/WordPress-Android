package org.wordpress.android.util;

import android.support.v4.app.FragmentTransaction;

import org.wordpress.android.R;

/**
 * Created by nbradbury on 3/8/14.
 */
public class FragmentUtils {

    /*
     * adds a slide-in/slide-out transition to the passed fragment transaction - note
     * that for this to work, it MUST be called right after the transaction is created
     * http://stackoverflow.com/questions/4817900/android-fragments-and-animation/17488542#17488542
     */
    public static void setTransition(FragmentTransaction ft) {
        if (ft == null)
            return;
        ft.setCustomAnimations(R.anim.transition_enter, R.anim.transition_exit,
                               R.anim.transition_pop_enter, R.anim.transition_pop_exit);
        // ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
    }
}
