package org.wordpress.android.fluxc.network.rest.wpcom.account;

import org.wordpress.android.fluxc.network.Response;

import java.util.List;

public class SubscriptionRestResponse implements Response {
    public class SubscriptionsResponse {
        public List<SubscriptionRestResponse> subscriptions;
    }

    public class DeliveryMethod {
        public class Email {
            public boolean send_posts;
            public boolean send_comments;
            public String post_delivery_frequency;
        }

        public class Notification {
            public boolean send_posts;
        }

        public Email email;
        public Notification notification;
    }

    public class Meta {
        public class Data {
            public class Site {
                public String name;
            }

            public Site site;
        }

        public Data data;
    }

    public String ID;
    public String blog_ID;
    public String feed_ID;
    public String URL;
    public DeliveryMethod delivery_methods;
    public Meta meta;
}
