package org.wordpress.android.ui;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.ProgressBar;

import org.wordpress.android.fluxc.utils.MediaUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.helpers.WPWebChromeClient;

import static android.app.Activity.RESULT_OK;
import static org.wordpress.android.ui.RequestCodes.WEB_CHROME_CLIENT_FILE_PICKER;

public class WPWebChromeClientWithFileChooser extends WPWebChromeClient {
    private ValueCallback<Uri[]> mFilePathCallback;
    private final OnShowFileChooserListener mOnShowFileChooserListener;

    public WPWebChromeClientWithFileChooser(Activity activity, View view, int defaultPoster,
                                            ProgressBar progressBar,
                                            OnShowFileChooserListener onShowFileChooserListener) {
        super(activity, view, defaultPoster, progressBar);
        this.mOnShowFileChooserListener = onShowFileChooserListener;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                               FileChooserParams fileChooserParams) {
        this.mFilePathCallback = filePathCallback;

        // Check if MODE_OPEN_MULTIPLE is specified
        Boolean canMultiselect = false;
        if (fileChooserParams.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
            canMultiselect = true;
        }

        Intent intent = fileChooserParams.createIntent();
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, canMultiselect);

        String[] acceptableMimeTypes = fileChooserParams.getAcceptTypes();
        int acceptableMimeTypesLength = acceptableMimeTypes.length;

        // Sets an explicit MIME data type that states that all MIME types are supported.
        intent.setType("*/*");
        // Creates values for Intent.EXTRA_MIME_TYPES with the MIME types that are currently acceptable.
        String[] resolvedMimeTypes = new String[acceptableMimeTypesLength];
        String resolvedMimeType = null;

        for (int index = 0; index < acceptableMimeTypesLength; index++) {
            String acceptableMimeType = acceptableMimeTypes[index];

            /**
             * The fileChooserParams.getAcceptTypes() API states that the it returns an array of acceptable MIME
             * types. The returned MIME type could be partial such as audio/* . Currently, there are plugins that
             * return extensions when the form input type is utilized instead of
             *  MIME types. The logic below is to accommodate the use cases by utilizing the extension to resolve
             *  the appropriate Mime type with MediaUtils.getMimeTypeForExtension().
             *
             *  N.B The condition below ensures that mime-types that have dots in them (eg. application/vnd.ms-excel)
             *  are not accepted.
             */
            if (acceptableMimeType.contains(".") && !acceptableMimeType.contains("/")) {
                String extension = acceptableMimeType.replace(".", "");
                resolvedMimeType =
                        MediaUtils.getMimeTypeForExtension(extension);
            } else if (acceptableMimeType.contains("/")) {
                resolvedMimeType = acceptableMimeType;
            }

            if (resolvedMimeType != null) {
                resolvedMimeTypes[index] = resolvedMimeType;
            } else {
                AppLog.w(T.EDITOR,
                        "MediaUtils.getMimeTypeForExtension failed to resolve the ${acceptableMimeType} MIME type");
            }
        }

        // Uses Intent.EXTRA_MIME_TYPES to the MIME types that should be acceptable.
        intent.putExtra(Intent.EXTRA_MIME_TYPES, resolvedMimeTypes);

        if (mOnShowFileChooserListener != null) {
            mOnShowFileChooserListener.startActivityForFileChooserResult(intent, WEB_CHROME_CLIENT_FILE_PICKER);
        }

        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        Uri[] selectedUris = null;

        if (intent != null && resultCode == RESULT_OK && requestCode == WEB_CHROME_CLIENT_FILE_PICKER) {
            // if ClipData is not empty that means there are multiple files.
            if (intent.getClipData() != null) {
                // process multiple files
                int clipDataItemCount = intent.getClipData().getItemCount();
                selectedUris = new Uri[clipDataItemCount];
                for (int index = 0; index < clipDataItemCount; index++) {
                    selectedUris[index] = intent.getClipData().getItemAt(index).getUri();
                }
            } else if (intent.getData() != null) {
                // process the single file
                selectedUris = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            }
        }

        // Once onShowFileChooser has been triggered and mFilePathCallback is set, the onReceiveValue function has to
        // be called with a value (null when no file is selected) because other form input controls will not function.
        // They will not trigger the File Picker since it is waiting on the result of the previous file request.
        if (mFilePathCallback != null) {
            mFilePathCallback.onReceiveValue(selectedUris);
        }
    }

    public interface OnShowFileChooserListener {
        void startActivityForFileChooserResult(Intent intent, int requestCode);
    }
}
