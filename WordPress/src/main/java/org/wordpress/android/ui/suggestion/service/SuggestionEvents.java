package org.wordpress.android.ui.suggestion.service;

public class SuggestionEvents {
    public static class SuggestionListUpdated {
        public final int mRemoteBlogId;
        SuggestionListUpdated(int remoteBlogId) {
            mRemoteBlogId = remoteBlogId;
        }
    }
}
