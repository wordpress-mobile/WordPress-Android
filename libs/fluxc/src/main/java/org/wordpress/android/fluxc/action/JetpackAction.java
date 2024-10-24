package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.JetpackStore.ActivateStatsModulePayload;

@ActionEnum
public enum JetpackAction implements IAction {
    @Action(payloadType = SiteModel.class)
    INSTALL_JETPACK,
    @Action(payloadType = ActivateStatsModulePayload.class)
    ACTIVATE_STATS_MODULE
}
