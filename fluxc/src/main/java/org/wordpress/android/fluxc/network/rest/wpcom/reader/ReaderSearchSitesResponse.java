package org.wordpress.android.fluxc.network.rest.wpcom.reader;

import android.support.annotation.NonNull;

import com.google.gson.annotations.JsonAdapter;

import org.wordpress.android.fluxc.model.ReaderSiteModel;

import java.util.List;

@JsonAdapter(ReaderSearchSitesDeserializer.class)
public class ReaderSearchSitesResponse {
    public @NonNull List<ReaderSiteModel> sites;
}
