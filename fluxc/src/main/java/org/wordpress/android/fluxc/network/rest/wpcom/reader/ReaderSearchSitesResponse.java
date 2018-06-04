package org.wordpress.android.fluxc.network.rest.wpcom.reader;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.SitesModel;

public class ReaderSearchSitesResponse {
    public boolean canLoadMore;
    public int offset;
    public @NonNull SitesModel sites;
}
