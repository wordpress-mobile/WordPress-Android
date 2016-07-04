package org.wordpress.android.ui.menus.views;

import org.wordpress.android.models.MenuModel;

public interface MenuSaveProgressListener {
    void onSaveCompleted(boolean successfully); // use this method to signal MenuAddEditRemoveView that the saving operation has ended
    void onSaveStarted(MenuModel menu); // use this method to signal MenuAddEditRemoveView that the saving operation has just started
}
