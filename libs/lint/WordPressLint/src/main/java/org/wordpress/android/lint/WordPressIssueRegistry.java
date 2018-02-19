package org.wordpress.android.lint;


import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;

import java.util.Arrays;
import java.util.List;

public class WordPressIssueRegistry extends IssueRegistry {

    @Override
    public List<Issue> getIssues() {
        return Arrays.asList(WordPressRtlCodeDetector.SET_PADDING, WordPressRtlCodeDetector.SET_MARGIN);
    }
}
