package org.wordpress.android.util;

public class CoreEvents {
    public static class MainViewPagerScrolled {
        public final float mXOffset;
        public MainViewPagerScrolled(float xOffset) {
            mXOffset = xOffset;
        }
    }
}
