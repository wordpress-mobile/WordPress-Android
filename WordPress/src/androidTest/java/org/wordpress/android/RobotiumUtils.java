package org.wordpress.android;

import android.view.View;

import com.robotium.solo.Solo;

import java.util.ArrayList;

public class RobotiumUtils {
    public static void clickOnClassId(Solo solo, String className, String id) {
        ArrayList<View> views = solo.getCurrentViews();
        for (View view : views) {
            if (view.getClass().toString().endsWith(className)) {
                if (view.toString().contains(id)) {
                    solo.clickOnView(view);
                }
            }
        }
    }

    public static void clickOnId(Solo solo, String id) {
        ArrayList<View> views = solo.getCurrentViews();
        for (View view : views) {
            if (view.toString().contains(id)) {
                solo.clickOnView(view);
            }
        }
    }
}
