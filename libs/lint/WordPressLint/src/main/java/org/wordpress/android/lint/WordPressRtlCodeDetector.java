package org.wordpress.android.lint;

import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector.UastScanner;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.TextFormat;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.UExpression;

import java.util.Collections;
import java.util.List;

public class WordPressRtlCodeDetector extends Detector implements UastScanner {

    static final Issue SET_PADDING = Issue.create(
            // ID: used in @SuppressLint warnings etc
            "RtlSetPadding",

            // Title -- shown in the IDE's preference dialog, as category headers in the
            // Analysis results window, etc
            "Using setPadding instead of setPaddingRelative.",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "For RtL compatibility, use setPaddingRelative" +
                    "or ViewCompat.setPaddingRelative() when setting left/right padding.",
            Category.RTL,
            6,
            Severity.ERROR,
            new Implementation(WordPressRtlCodeDetector.class, Scope.JAVA_FILE_SCOPE));

    static final Issue SET_MARGIN = Issue.create(
            "RtlSetMargins",
            "Incorrect use of setMargin in RtL context.",
            "For RtL compatibility, use setMarginStart() and setMarginEnd()" +
                    " or their MarginLayoutParamsCompat version, when setting left/right margins.",
            Category.RTL,
            6,
            Severity.ERROR,
            new Implementation(WordPressRtlCodeDetector.class, Scope.JAVA_FILE_SCOPE));

    static final Issue GET_PADDING = Issue.create(
            "RtlGetPadding",
            "Using getPaddingLeft/Right instead of getPaddingStart/End.",
            "For RtL compatibility, use getPaddingStart() and getPaddingEnd()" +
                    " or their ViewCompat version, when getting left/right padding.",
            Category.RTL,
            6,
            Severity.ERROR,
            new Implementation(WordPressRtlCodeDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(UCallExpression.class);
    }

    @Override
    public UElementHandler createUastHandler(JavaContext context) {

        return new UElementHandler() {

            @Override
            public void visitCallExpression(UCallExpression uCallExpression) {
                String methodName = uCallExpression.getMethodName();

                if (methodName == null) return;

                if (methodName.equals("setPadding") || methodName.equals("setMargins")) {

                    //trying to make sure it's the right method
                    if (uCallExpression.getValueArgumentCount() == 4) {

                        UExpression left = uCallExpression.getValueArguments().get(0);
                        UExpression right = uCallExpression.getValueArguments().get(2);

                        //we can't evaluate variables that are passed to the method, so the next best thing is to compare
                        //passed values as strings. We are looking for cases where padding or margin are not same.
                        if (!left.asRenderString().equals(right.asRenderString())) {
                            Issue issueToReport;
                            if (methodName.equals("setPadding")) {
                                issueToReport = SET_PADDING;
                            } else {
                                issueToReport = SET_MARGIN;
                            }
                            context.report(issueToReport, uCallExpression, context.getLocation(uCallExpression), issueToReport.getExplanation(TextFormat.TEXT));
                        }
                    }
                } else if (methodName.equals("getPaddingLeft") || methodName.equals("getPaddingRight")) {
                    context.report(GET_PADDING, uCallExpression, context.getLocation(uCallExpression), GET_PADDING.getExplanation(TextFormat.TEXT));
                }
            }
        };
    }
}
