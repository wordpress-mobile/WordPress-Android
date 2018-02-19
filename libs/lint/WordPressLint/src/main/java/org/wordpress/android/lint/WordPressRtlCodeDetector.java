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
            "Using setPadding in RtL context.",

            // Full explanation of the issue; you can use some markdown markup such as
            // `monospace`, *italic*, and **bold**.
            "For compatibility with RtL layout direction, use setPaddingRelative" +
                    "or ViewCompat.setPaddingRelative() when setting left/right padding.",
            Category.RTL,
            6,
            Severity.ERROR,
            new Implementation(WordPressRtlCodeDetector.class, Scope.JAVA_FILE_SCOPE));

    static final Issue SET_MARGIN = Issue.create(
            "RtlSetMargins",
            "Using setPadding in RtL context.",
            "For compatibility with RtL layout direction, use setMarginStart() and setMarginStart()" +
                    " or MarginLayoutParamsCompat version, when setting left/right margins.",
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

                if (methodName != null && (methodName.equals("setPadding") || methodName.equals("setMargins"))) {

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
                }
            }
        };
    }
}
