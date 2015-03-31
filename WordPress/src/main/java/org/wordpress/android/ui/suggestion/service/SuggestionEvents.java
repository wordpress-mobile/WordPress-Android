package org.wordpress.android.ui.suggestion.service;

public class SuggestionEvents {
    public static class SuggestionNameListUpdated {
        public final int mRemoteBlogId;
        SuggestionNameListUpdated(int remoteBlogId) {
            mRemoteBlogId = remoteBlogId;
        }
    }

    public static class SuggestionTagListUpdated {
        public final int mRemoteBlogId;
        SuggestionTagListUpdated(int remoteBlogId) {
            mRemoteBlogId = remoteBlogId;
        }
    }
}
