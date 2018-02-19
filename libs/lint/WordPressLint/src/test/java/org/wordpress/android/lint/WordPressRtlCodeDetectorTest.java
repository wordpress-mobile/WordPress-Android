package org.wordpress.android.lint;

import com.android.tools.lint.checks.infrastructure.LintDetectorTest;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Issue;

import java.util.Arrays;
import java.util.List;

public class WordPressRtlCodeDetectorTest extends LintDetectorTest {

    public void testSetPadding() {
        lint().allowCompilationErrors(true).files(
                java("" +
                        "package test.pkg;\n" +
                        "public class TestClass1 {\n" +
                        "    // In a comment, mentioning \"lint\" has no effect\n" +
                        "    private static String s1 = \"Ignore non-word usages: linting\";\n" +
                        "    private static String s2 = \"Let's say it: lint\";\n" +
                        "public void testMethod() {\n" +
                        "            setPadding(0, 1, 0, 2);\n" + //should be fine
                        "            setPadding(1, 2, 1, 2);\n" + //still ok (left and right padding is the same)
                        "            setPadding(1, 1, 2, 2);\n" + //oh-oh, Danger zone!
                        "        }\n" +
                        "}"))
                .run()
                .expect("src/test/pkg/TestClass1.java:9: Error: For RtL compatibility," +
                        " use setPaddingRelativeor ViewCompat.setPaddingRelative() when setting left/right padding." +
                        " [RtlSetPadding]\n" +
                        "            setPadding(1, 1, 2, 2);\n" +
                        "            ~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings");
    }


    public void testSetMargins() {
        lint().allowCompilationErrors(true).files(
                java("" +
                        "package test.pkg;\n" +
                        "public class TestClass1 {\n" +
                        "    // In a comment, mentioning \"lint\" has no effect\n" +
                        "    private static String s1 = \"Ignore non-word usages: linting\";\n" +
                        "    private static String s2 = \"Let's say it: lint\";\n" +
                        "public void testMethod() {\n" +
                        "            setMargins(0, 1, 0, 2);\n" + //should be fine
                        "            setMargins(1, 2, 1, 2);\n" + //still ok (left and right margin is the same)
                        "            setMargins(1, 1, 2, 2);\n" + //oh-oh, Danger zone!
                        "        }\n" +
                        "}"))
                .run()
                .expect("src/test/pkg/TestClass1.java:9: Error: For RtL compatibility," +
                        " use setMarginStart() and setMarginEnd() or their MarginLayoutParamsCompat version, when setting" +
                        " left/right margins. [RtlSetMargins]\n" +
                        "            setMargins(1, 1, 2, 2);\n" +
                        "            ~~~~~~~~~~~~~~~~~~~~~~\n" +
                        "1 errors, 0 warnings\n");
    }

    public void testGetPadding() {
        lint().allowCompilationErrors(true).files(
                java("" +
                        "package test.pkg;\n" +
                        "public class TestClass1 {\n" +
                        "    // In a comment, mentioning \"lint\" has no effect\n" +
                        "    private static String s1 = \"Ignore non-word usages: linting\";\n" +
                        "    private static String s2 = \"Let's say it: lint\";\n" +
                        "public void testMethod() {\n" +
                        "            getPaddingRight();\n" + //NG
                        "            getPaddingLeft();\n" + //NG
                        "            getPaddingBottom();\n" + //OK
                        "            getPaddingTop();\n" + //OK
                        "        }\n" +
                        "}"))
                .run()
                .expect("src/test/pkg/TestClass1.java:7: Error: For RtL compatibility, use getPaddingStart() and getPaddingEnd() or their ViewCompat version, when getting left/right padding. [RtlGetPadding]\n" +
                        "            getPaddingRight();\n" +
                        "            ~~~~~~~~~~~~~~~~~\n" +
                        "src/test/pkg/TestClass1.java:8: Error: For RtL compatibility, use getPaddingStart() and getPaddingEnd() or their ViewCompat version, when getting left/right padding. [RtlGetPadding]\n" +
                        "            getPaddingLeft();\n" +
                        "            ~~~~~~~~~~~~~~~~\n" +
                        "2 errors, 0 warnings\n");
    }


    @Override
    protected Detector getDetector() {
        return new WordPressRtlCodeDetector();
    }

    @Override
    protected List<Issue> getIssues() {
        return Arrays.asList(WordPressRtlCodeDetector.SET_PADDING, WordPressRtlCodeDetector.SET_MARGIN,WordPressRtlCodeDetector.GET_PADDING);
    }

}
