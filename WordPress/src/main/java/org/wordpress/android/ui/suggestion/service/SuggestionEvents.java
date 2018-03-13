package org.wordpress.android.ui.suggestion.service;

public class SuggestionEvents {
    public static class SuggestionNameListUpdated {
        public final long mRemoteBlogId;

        SuggestionNameListUpdated(long remoteBlogId) {
            mRemoteBlogId = remoteBlogId;
        }
    }

    public static class SuggestionTagListUpdated {
        public final long mRemoteBlogId;

        SuggestionTagListUpdated(long remoteBlogId) {
            mRemoteBlogId = remoteBlogId;
        }
    }
}
