package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;

@ActionEnum
public enum TaxonomyAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_CATEGORIES,
    @Action(payloadType = SiteModel.class)
    FETCH_TAGS,
    @Action(payloadType = FetchTermsPayload.class)
    FETCH_TERMS,

    // Remote responses
    @Action(payloadType = FetchTermsResponsePayload.class)
    FETCHED_TERMS

    // Local actions
}

