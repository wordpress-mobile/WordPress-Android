package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;

@ActionEnum
public enum JetpackAction implements IAction {
    @Action(payloadType = JetpackAction.class)
    INSTALL_JETPACK,
    @Action(payloadType = JetpackAction.class)
    INSTALLED_JETPACK
}
