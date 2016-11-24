package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.TermModel;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.FetchTermsResponsePayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.InstantiateTermPayload;
import org.wordpress.android.fluxc.store.TaxonomyStore.RemoteTermPayload;

@ActionEnum
public enum TaxonomyAction implements IAction {
    // Remote actions
    @Action(payloadType = SiteModel.class)
    FETCH_CATEGORIES,
    @Action(payloadType = SiteModel.class)
    FETCH_TAGS,
    @Action(payloadType = FetchTermsPayload.class)
    FETCH_TERMS,
    @Action(payloadType = RemoteTermPayload.class)
    FETCH_TERM,
    @Action(payloadType = RemoteTermPayload.class)
    PUSH_TERM,

    // Remote responses
    @Action(payloadType = FetchTermsResponsePayload.class)
    FETCHED_TERMS,
    @Action(payloadType = FetchTermResponsePayload.class)
    FETCHED_TERM,
    @Action(payloadType = RemoteTermPayload.class)
    PUSHED_TERM,

    // Local actions
    @Action(payloadType = SiteModel.class)
    INSTANTIATE_CATEGORY,
    @Action(payloadType = SiteModel.class)
    INSTANTIATE_TAG,
    @Action(payloadType = InstantiateTermPayload.class)
    INSTANTIATE_TERM,
    @Action(payloadType = TermModel.class)
    UPDATE_TERM
}

