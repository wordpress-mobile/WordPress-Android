package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesPayload;
import org.wordpress.android.fluxc.store.ReaderStore.ReaderSearchSitesResponsePayload;

@ActionEnum
public enum ReaderAction implements IAction {
    // Remote actions
    @Action(payloadType = ReaderSearchSitesPayload.class)
    READER_SEARCH_SITES,

    // Remote responses
    @Action(payloadType = ReaderSearchSitesResponsePayload.class)
    READER_SEARCHED_SITES
}
