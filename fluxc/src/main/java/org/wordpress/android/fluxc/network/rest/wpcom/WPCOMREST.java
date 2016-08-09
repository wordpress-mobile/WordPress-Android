package org.wordpress.android.fluxc.network.rest.wpcom;


public class WPCOMREST {
    // Top-level endpoints
    public static MeEndpoint me = new MeEndpoint();
    public static SitesEndpoint sites = new SitesEndpoint();
    public static UsersEndpoint users = new UsersEndpoint();

    public static class MeEndpoint extends WPComEndpoint {
        private static final String ME_ENDPOINT = "/me/";

        public WPComEndpoint settings = new WPComEndpoint(ME_ENDPOINT + "settings/");
        public WPComEndpoint sites = new WPComEndpoint(ME_ENDPOINT + "sites/");

        private MeEndpoint() {
            super(ME_ENDPOINT);
        }
    }

    public static class SitesEndpoint extends WPComEndpoint {
        private static final String SITES_ENDPOINT = "/sites/";

        public WPComEndpoint new_ = new WPComEndpoint(SITES_ENDPOINT + "new/");

        private SitesEndpoint() {
            super("/sites/");
        }

        public SiteEndpoint site(int siteId) {
            return new SiteEndpoint(getEndpoint(), siteId);
        }

        public static class SiteEndpoint extends WPComEndpoint {
            public final PostsEndpoint posts = new PostsEndpoint(getEndpoint());

            private SiteEndpoint(String previousEndpoint, long siteId) {
                super(previousEndpoint, siteId);
            }

            public static class PostsEndpoint extends WPComEndpoint {
                public WPComEndpoint new_ = new WPComEndpoint(getEndpoint() + "new/");

                private PostsEndpoint(String previousEndpoint) {
                    super(previousEndpoint + "posts/");
                }

                public PostEndpoint post(long postId) {
                    return new PostEndpoint(getEndpoint(), postId);
                }

                public static class PostEndpoint extends WPComEndpoint {
                    public final WPComEndpoint delete = new WPComEndpoint(getEndpoint() + "delete/");

                    private PostEndpoint(String previousEndpoint, long postId) {
                        super(previousEndpoint, postId);
                    }
                }
            }
        }
    }

    public static class UsersEndpoint extends WPComEndpoint {
        private static final String USERS_ENDPOINT = "/users/";

        public WPComEndpoint new_ = new WPComEndpoint(USERS_ENDPOINT + "new/");

        private UsersEndpoint() {
            super(USERS_ENDPOINT);
        }
    }
}
