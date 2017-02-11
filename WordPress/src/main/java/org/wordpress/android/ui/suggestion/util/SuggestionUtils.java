package org.wordpress.android.ui.suggestion.util;

import android.content.Context;

import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.models.Tag;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.adapters.TagSuggestionAdapter;
import org.wordpress.android.util.SiteUtils;

import java.util.List;

public class SuggestionUtils {

    public static SuggestionAdapter setupSuggestions(SiteModel site, Context context,
                                                     SuggestionServiceConnectionManager serviceConnectionManager) {
        return SuggestionUtils.setupSuggestions(site.getSiteId(), context, serviceConnectionManager,
                SiteUtils.isAccessibleViaWPComAPI(site));
    }

    public static SuggestionAdapter setupSuggestions(final long siteId, Context context,
                                                     SuggestionServiceConnectionManager serviceConnectionManager,
                                                     boolean isDotcomFlag) {
        if (!isDotcomFlag) {
            return null;
        }

        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(context);

        List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(siteId);
        // if the suggestions are not stored yet, we want to trigger an update for it
        if (suggestions.isEmpty()) {
            serviceConnectionManager.bindToService();
        }
        suggestionAdapter.setSuggestionList(suggestions);
        return suggestionAdapter;
    }

    public static TagSuggestionAdapter setupTagSuggestions(SiteModel site, Context context,
                                                           SuggestionServiceConnectionManager serviceConnectionManager) {
        return SuggestionUtils.setupTagSuggestions(site.getSiteId(), context, serviceConnectionManager,
                SiteUtils.isAccessibleViaWPComAPI(site));
    }

    public static TagSuggestionAdapter setupTagSuggestions(final long siteId, Context context,
                                                           SuggestionServiceConnectionManager serviceConnectionManager,
                                                           boolean isDotcomFlag) {
        if (!isDotcomFlag) {
            return null;
        }

        TagSuggestionAdapter tagSuggestionAdapter = new TagSuggestionAdapter(context);

        List<Tag> tags = SuggestionTable.getTagsForSite(siteId);
        // if the tags are not stored yet, we want to trigger an update for it
        if (tags.isEmpty()) {
            serviceConnectionManager.bindToService();
        }
        tagSuggestionAdapter.setTagList(tags);
        return tagSuggestionAdapter;
    }
}
