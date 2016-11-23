package org.wordpress.android.ui.suggestion.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.wordpress.android.ui.suggestion.service.SuggestionService;

public class SuggestionServiceConnectionManager implements ServiceConnection {

    private final Context mContext;
    private final int mRemoteBlogId;
    private boolean mAttemptingToBind = false;
    private boolean mBindCalled = false;

    public SuggestionServiceConnectionManager(Context context, int remoteBlogId) {
        mContext = context;
        mRemoteBlogId = remoteBlogId;
    }

    public void bindToService() {
        if (!mAttemptingToBind) {
            mAttemptingToBind = true;
            mBindCalled = true;
            Intent intent = new Intent(mContext, SuggestionService.class);
            mContext.bindService(intent, this, Context.BIND_AUTO_CREATE);
        }
    }

    public void unbindFromService() {
        mAttemptingToBind = false;
        if (mBindCalled) {
            mContext.unbindService(this);
            mBindCalled = false;
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        SuggestionService.SuggestionBinder b = (SuggestionService.SuggestionBinder) iBinder;
        SuggestionService suggestionService = b.getService();

        suggestionService.updateSuggestions(mRemoteBlogId);
        suggestionService.updateTags(mRemoteBlogId);

        mAttemptingToBind = false;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        // noop
    }
}
