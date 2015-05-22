package org.wordpress.android.util;

public class CoreEvents {
    public static class BlogListChanged {}
    public static class RestApiUnauthorized {}
    public static class UserSignedOutWordPressCom {}
    public static class UserSignedOutCompletely {}
    public static class InvalidCredentialsDetected {}
    public static class InvalidSslCertificateDetected {}
    public static class LoginLimitDetected {}
    public static class TwoFactorAuthenticationDetected {}
    public static class MainViewPagerScrolling {
        public final int mXOffset;
        public final int mYOffset;
        public MainViewPagerScrolling(int xOffset, int yOffset) {
            mXOffset = xOffset;
            mYOffset = yOffset;
        }
    }
}
