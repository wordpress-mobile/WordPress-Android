package org.wordpress.android.ui.accounts.login.nav;

import de.greenrobot.event.EventBus;

public abstract class LoginStateHandler {
    public static class ActionLogin {
        public static void emit() {
            EventBus.getDefault().post(new LoginStateHandler.ActionLogin());
        }
    }

    public static class ActionSignup {
        public static void emit() {
            EventBus.getDefault().post(new LoginStateHandler.ActionSignup());
        }
    }

    public static class ActionGotEmail {
        public final String email;

        public static void emit(String email) {
            EventBus.getDefault().post(new LoginStateHandler.ActionGotEmail(email));
        }

        private ActionGotEmail(String email) {
            this.email = email;
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(Object event) {
        throw new RuntimeException("Event \"" + event.getClass().getSimpleName() + "\" not supported in this state!");
    }
}

