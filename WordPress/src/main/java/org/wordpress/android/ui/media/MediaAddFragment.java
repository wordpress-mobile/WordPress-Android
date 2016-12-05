package org.wordpress.android.ui.media;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.greenrobot.eventbus.Subscribe;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.MediaAction;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.models.MediaUploadState;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.media.WordPressMediaUtils.LaunchCameraCallback;
import org.wordpress.android.ui.media.services.MediaUploadService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.MediaUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.MediaFile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

/**
 * An invisible fragment in charge of launching the right intents to camera, video, and image library.
 * Also queues up media for upload and listens to notifications from the upload service.
 */
public class MediaAddFragment extends Fragment implements LaunchCameraCallback {
    private static final String BUNDLE_MEDIA_CAPTURE_PATH = "mediaCapturePath";
    private String mMediaCapturePath = "";

    @Inject MediaStore mMediaStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        if (savedInstanceState == null) {
            if (getArguments() != null) {
                mSite = (SiteModel) getArguments().getSerializable(WordPress.SITE);
            } else {
                mSite = (SiteModel) getActivity().getIntent().getSerializableExtra(WordPress.SITE);
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(getActivity(), R.string.blog_not_found, ToastUtils.Duration.SHORT);
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // This view doesn't really matter as this fragment is invisible

        if (savedInstanceState != null && savedInstanceState.getString(BUNDLE_MEDIA_CAPTURE_PATH) != null)
            mMediaCapturePath = savedInstanceState.getString(BUNDLE_MEDIA_CAPTURE_PATH);

        return inflater.inflate(R.layout.actionbar_add_media_cell, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMediaCapturePath != null && !mMediaCapturePath.equals("")) {
            outState.putString(BUNDLE_MEDIA_CAPTURE_PATH, mMediaCapturePath);
        }
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public void onStart() {
        super.onStart();
        // register context for change in connection status
        getActivity().registerReceiver(mReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        mDispatcher.register(this);
    }

    @Override
    public void onStop() {
        getActivity().unregisterReceiver(mReceiver);
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        resumeMediaUploadService();
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
                // Coming from zero connection. Re-register upload intent.
                resumeMediaUploadService();
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (data != null || requestCode == RequestCodes.TAKE_PHOTO ||
                requestCode == RequestCodes.TAKE_VIDEO) {
            String path;

            switch (requestCode) {
                case RequestCodes.PICTURE_LIBRARY:
                case RequestCodes.VIDEO_LIBRARY:
                    Uri imageUri = data.getData();
                    fetchMedia(imageUri);
                    break;
                case RequestCodes.TAKE_PHOTO:
                    if (resultCode == Activity.RESULT_OK) {
                        path = mMediaCapturePath;
                        mMediaCapturePath = null;
                        queueFileForUpload(path);
                    }
                    break;
                case RequestCodes.TAKE_VIDEO:
                    if (resultCode == Activity.RESULT_OK) {
                        path = getRealPathFromURI(MediaUtils.getLastRecordedVideoUri(getActivity()));
                        queueFileForUpload(path);
                    }
                    break;
            }
        }
    }

    private void fetchMedia(Uri mediaUri) {
        if (!MediaUtils.isInMediaStore(mediaUri)) {
            // Create an AsyncTask to download the file
            new DownloadMediaTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, mediaUri);
        } else {
            // It is a regular local media file
            String path = getRealPathFromURI(mediaUri);
            queueFileForUpload(path);
        }
    }

    private String getRealPathFromURI(Uri uri) {
        String path;
        if ("content".equals(uri.getScheme())) {
            path = getRealPathFromContentURI(uri);
        } else if ("file".equals(uri.getScheme())) {
            path = uri.getPath();
        } else {
            path = uri.toString();
        }
        return path;
    }

    private String getRealPathFromContentURI(Uri contentUri) {
        if (contentUri == null)
            return null;

        String[] proj = { android.provider.MediaStore.Images.Media.DATA };
        CursorLoader loader = new CursorLoader(getActivity(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();

        if (cursor == null)
            return null;

        int column_index = cursor.getColumnIndex(proj[0]);
        if (column_index == -1) {
            cursor.close();
            return null;
        }

        String path;
        if (cursor.moveToFirst()) {
            path = cursor.getString(column_index);
        } else {
            path = null;
        }

        cursor.close();
        return path;
    }

    private void queueFileForUpload(String path) {
        if (path == null || path.equals("")) {
            Toast.makeText(getActivity(), "Error opening file", Toast.LENGTH_SHORT).show();
            return;
        }

        File file = new File(path);
        if (!file.exists()) {
            return;
        }

        String mimeType = MediaUtils.getMediaFileMimeType(file);
        String fileName = MediaUtils.getMediaFileName(file, mimeType);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setBlogId(String.valueOf(mSite.getId()));
        mediaFile.setFileName(fileName);
        mediaFile.setFilePath(path);
        mediaFile.setUploadState("queued");
        mediaFile.setDateCreatedGMT(System.currentTimeMillis());
        mediaFile.setMediaId(String.valueOf(System.currentTimeMillis()));
        if (mimeType != null && mimeType.startsWith("image")) {
            // get width and height
            BitmapFactory.Options bfo = new BitmapFactory.Options();
            bfo.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, bfo);
            mediaFile.setWidth(bfo.outWidth);
            mediaFile.setHeight(bfo.outHeight);
        }

        if (!TextUtils.isEmpty(mimeType)) {
            mediaFile.setMimeType(mimeType);
        }
        saveMedia(mediaFile);
//        WordPress.wpDB.saveMediaFile(mediaFile);
//        EventBus.getDefault().post(new MediaChanged(String.valueOf(mSite.getId()), mediaFile.getMediaId()));
        startMediaUploadService();
    }

    private MediaModel fromMediaFile(MediaFile file) {
        MediaModel mediaModel = new MediaModel();
        mediaModel.setFileName(file.getFileName());
        mediaModel.setFilePath(file.getFilePath());
        mediaModel.setFileExtension(org.wordpress.android.fluxc.utils.MediaUtils.getExtension(file.getFilePath()));
        mediaModel.setMimeType(file.getMimeType());
        mediaModel.setThumbnailUrl(file.getThumbnailURL());
        mediaModel.setTitle(file.getTitle());
        mediaModel.setDescription(file.getDescription());
        mediaModel.setCaption(file.getCaption());
        mediaModel.setMediaId(Long.valueOf(file.getMediaId()));
        return mediaModel;
    }

    private void saveMedia(MediaFile media) {
        List<MediaModel> mediaList = new ArrayList<>();
        mediaList.add(fromMediaFile(media));
        MediaStore.MediaListPayload payload = new MediaStore.MediaListPayload(MediaAction.UPDATE_MEDIA, mSite, mediaList);
        mDispatcher.dispatch(MediaActionBuilder.newUpdateMediaAction(payload));
    }

    private void startMediaUploadService() {
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            Intent intent = new Intent(getActivity(), MediaUploadService.class);
            intent.putExtra(WordPress.SITE, mSite);
            getActivity().startService(intent);
        }
    }

    private void resumeMediaUploadService() {
        startMediaUploadService();
    }

    @Override
    public void onMediaCapturePathReady(String mediaCapturePath) {
        mMediaCapturePath = mediaCapturePath;
    }

    public void launchCamera() {
        WordPressMediaUtils.launchCamera(this, BuildConfig.APPLICATION_ID, this);
    }

    public void launchVideoCamera() {
        WordPressMediaUtils.launchVideoCamera(this);
    }

    public void launchVideoLibrary() {
        WordPressMediaUtils.launchVideoLibrary(this);
    }

    public void launchPictureLibrary() {
        WordPressMediaUtils.launchPictureLibrary(this);
    }

    public void addToQueue(long mediaId) {
        String blogId = String.valueOf(mSite.getId());
        WordPress.wpDB.updateMediaUploadState(blogId, String.valueOf(mediaId), MediaUploadState.QUEUED);
        startMediaUploadService();
    }

    public void uploadList(List<Uri> uriList) {
        for (Uri uri : uriList) {
            fetchMedia(uri);
        }
    }

    private class DownloadMediaTask extends AsyncTask<Uri, Integer, Uri> {
        @Override
        protected Uri doInBackground(Uri... uris) {
            Uri imageUri = uris[0];
            return MediaUtils.downloadExternalMedia(getActivity(), imageUri);
        }

        protected void onPostExecute(Uri newUri) {
            if (getActivity() == null)
                return;

            if (newUri != null) {
                String path = getRealPathFromURI(newUri);
                queueFileForUpload(path);
            }
            else
                Toast.makeText(getActivity(), getString(R.string.error_downloading_image), Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaChanged(MediaStore.OnMediaChanged event) {
        if (event.isError()) {
            AppLog.d(AppLog.T.MEDIA, "Received onMediaChanged error:" + event.error.message);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMediaUploaded(MediaStore.OnMediaUploaded event) {
        if (event.isError()) {
            AppLog.d(AppLog.T.MEDIA, "Received onMediaUploaded error:" + event.error.message);
        } else if (event.completed) {
            AppLog.d(AppLog.T.MEDIA, event.media.getTitle() + " upload complete");
        }
    }
}
