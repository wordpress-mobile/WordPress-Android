package org.wordpress.android.ui.media.services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Environment;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Downloads WP media to this device
 */

public class MediaDownloadService extends Service {
    private static final String KEY_LOCAL_MEDIA_ID = "local_media_id";

    @Inject Dispatcher mDispatcher;
    @Inject MediaStore mMediaStore;

    public static void startService(Context context, int localMediaId) {
        if (context == null) {
            return;
        }
        Intent intent = new Intent(context, MediaDownloadService.class);
        intent.putExtra(KEY_LOCAL_MEDIA_ID, localMediaId);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ((WordPress) getApplication()).component().inject(this);
        AppLog.i(AppLog.T.MEDIA, "Media Download Service > created");
        mDispatcher.register(this);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        AppLog.i(AppLog.T.MEDIA, "Media Download Service > destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            int localMediaId = intent.getIntExtra(KEY_LOCAL_MEDIA_ID, 0);
            saveMedia(localMediaId);
        }

        return START_NOT_STICKY;
    }

    private boolean saveMedia(int localMediaId) {
        MediaModel media = mMediaStore.getMediaWithLocalId(localMediaId);
        if (media == null) {
            return false;
        }

        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String filename = media.getFileName();
        OutputStream out = null;
        File file = new File(path, filename);
        try {
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                out = new FileOutputStream(file);
                return bmp.compress(Bitmap.CompressFormat.JPEG, 85, out);
            } catch (IOException e) {
                AppLog.e(AppLog.T.MEDIA, e);
                return false;
            }
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
