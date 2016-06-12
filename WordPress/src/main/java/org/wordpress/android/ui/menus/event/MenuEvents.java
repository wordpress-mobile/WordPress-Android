package org.wordpress.android.ui.menus.event;

public class MenuEvents {
    public static class AddMenuClicked {
        public final int mPosition;
        public AddMenuClicked(int pos) {
            mPosition = pos;
        }
    }
}
