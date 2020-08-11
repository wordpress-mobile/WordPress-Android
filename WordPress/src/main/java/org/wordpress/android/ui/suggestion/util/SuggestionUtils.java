package org.wordpress.android.ui.suggestion.util;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.util.SiteUtils;

import java.util.HashMap;
import java.util.List;

public class SuggestionUtils {
    private static HashMap<Long, Long> lastCallMap = new HashMap<>();
    private static final long MIN_MS_BEFORE_REFRESH = 60 * 1000;

    @Nullable
    public static SuggestionAdapter setupSuggestions(
            SiteModel site,
            Context context,
            @NonNull SuggestionServiceConnectionManager serviceConnectionManager
    ) {
        return SuggestionUtils.setupSuggestions(site.getSiteId(), context, serviceConnectionManager,
                                                SiteUtils.isAccessedViaWPComRest(site));
    }

    @Nullable
    public static SuggestionAdapter setupSuggestions(
            final long siteId,
            Context context,
            @NonNull SuggestionServiceConnectionManager serviceConnectionManager,
            boolean isWPComFlag
    ) {
        if (!isWPComFlag) {
            return null;
        }

        List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(siteId);
        if (shouldRefresh(siteId)) {
            serviceConnectionManager.bindToService();
        }

        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(context);
        suggestionAdapter.setSuggestionList(suggestions);

        return suggestionAdapter;
    }

    private static boolean shouldRefresh(final long siteId) {
        Long lastCallMs = lastCallMap.get(siteId);
        long currentMs = System.currentTimeMillis();
        boolean shouldMakeCall = lastCallMs == null || currentMs - lastCallMs > MIN_MS_BEFORE_REFRESH;

        if (shouldMakeCall) {
            lastCallMap.put(siteId, currentMs);
        }

        return shouldMakeCall;
    }
}
