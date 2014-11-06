package org.wordpress.android.ui.suggestion.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.SuggestionTable;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Suggestion;
import org.wordpress.android.ui.suggestion.adapters.SuggestionAdapter;
import org.wordpress.android.ui.suggestion.service.SuggestionService;

import java.util.List;

public class SuggestionUtils {

    public static SuggestionAdapter setupSuggestions(final int remoteBlogId, Context context, ServiceConnection serviceConnection) {
        Blog blog = WordPress.wpDB.getBlogForDotComBlogId(Integer.toString(remoteBlogId));
        boolean isDotComFlag = (blog != null && blog.isDotcomFlag());

        return SuggestionUtils.setupSuggestions(remoteBlogId, context, serviceConnection, isDotComFlag);
    }

    public static SuggestionAdapter setupSuggestions(final int remoteBlogId, Context context, ServiceConnection serviceConnection, boolean isDotcomFlag) {
        if (!isDotcomFlag) {
            return null;
        }

        SuggestionAdapter suggestionAdapter = new SuggestionAdapter(context);

        List<Suggestion> suggestions = SuggestionTable.getSuggestionsForSite(remoteBlogId);
        // if the suggestions are not stored yet, we want to trigger an update for it
        if (suggestions.isEmpty()) {
            Intent intent = new Intent(context, SuggestionService.class);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        }
        suggestionAdapter.setSuggestionList(suggestions);
        return suggestionAdapter;
    }

    public static ServiceConnection suggestionServiceConnection(final int remoteBlogId) {
        return new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder binder) {
                SuggestionService.SuggestionBinder b = (SuggestionService.SuggestionBinder) binder;
                SuggestionService suggestionService = b.getService();

                suggestionService.updateSuggestions(remoteBlogId);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) { }
        };
    }
}
