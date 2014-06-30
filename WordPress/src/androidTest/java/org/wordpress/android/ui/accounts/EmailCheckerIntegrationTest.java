package org.wordpress.android.ui.accounts;
import junit.framework.TestCase;

import org.wordpress.emailchecker.EmailChecker;

public class EmailCheckerIntegrationTest extends TestCase {
    // Intended to test integration, not the lib itself
    public void testIntegration() {
        EmailChecker emailChecker = new EmailChecker();
        emailChecker.suggestDomainCorrection("test");
        assert(true);
    }
}
