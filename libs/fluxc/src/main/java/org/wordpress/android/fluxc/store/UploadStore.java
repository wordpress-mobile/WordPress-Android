package org.wordpress.android.fluxc.store;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.action.PostAction;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.annotations.action.IAction;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadStore extends Store {
    @Inject
    public UploadStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void onRegister() {
        AppLog.d(T.API, "UploadStore onRegister");
    }

    // Ensure that events reach the UploadStore before their main stores (MediaStore, PostStore)
    @Subscribe(threadMode = ThreadMode.ASYNC, priority = 1)
    @Override
    public void onAction(Action action) {
        IAction actionType = action.getType();
        if (actionType instanceof PostAction) {
            onPostAction((PostAction) actionType, action.getPayload());
        }
        if (actionType instanceof MediaAction) {
            onMediaAction((MediaAction) actionType, action.getPayload());
        }
    }

    private void onPostAction(PostAction actionType, Object payload) {
        switch (actionType) {
            case PUSHED_POST:
                // TODO
                break;
        }
    }

    private void onMediaAction(MediaAction actionType, Object payload) {
        switch (actionType) {
            case UPLOAD_MEDIA:
                // TODO
                break;
            case UPLOADED_MEDIA:
                // TODO
                break;
        }
    }
}
