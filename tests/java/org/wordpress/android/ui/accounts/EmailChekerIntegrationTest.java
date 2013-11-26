package org.wordpress.android.ui.accounts;
import org.wordpress.emailchecker.*;
import junit.framework.TestCase;

public class EmailChekerIntegrationTest  extends TestCase {

    // Intended to test integration, not the lib itself
    public void testIntegration() {
        EmailChecker emailChecker = new EmailChecker();
        emailChecker.suggestDomainCorrection("test");
        assert(true);
    }
}
