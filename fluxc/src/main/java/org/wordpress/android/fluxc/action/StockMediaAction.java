package org.wordpress.android.fluxc.action;

import org.wordpress.android.fluxc.annotations.Action;
import org.wordpress.android.fluxc.annotations.ActionEnum;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.fluxc.store.StockMediaStore;

@ActionEnum
public enum StockMediaAction implements IAction {
    // Remote actions
    @Action(payloadType = StockMediaStore.FetchStockMediaListPayload.class)
    FETCH_STOCK_MEDIA,

    @Action(payloadType = StockMediaStore.FetchedStockMediaListPayload.class)
    FETCHED_STOCK_MEDIA,

    @Action(payloadType = StockMediaStore.UploadStockMediaPayload.class)
    UPLOAD_STOCK_MEDIA,

    @Action(payloadType = StockMediaStore.UploadedStockMediaPayload.class)
    UPLOADED_STOCK_MEDIA
}
