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
import org.wordpress.android.fluxc.utils.MimeTypes;
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

    @Override public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
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

        // Uses Intent.EXTRA_MIME_TYPES to pass multiple mime types if there are multiple MIME types.
        if (acceptableMimeTypesLength > 1) {
            // Sets an explicit MIME data type that states that all MIME types are supported.
            intent.setType("*/*");
            // Filter MIME types by Intent.EXTRA_MIME_TYPES with the types that are currently acceptable.
            String[] resolvedMimeTypes = new String[acceptableMimeTypes.length];
            String resolvedMimeType = null;

            for (int index = 0; index < acceptableMimeTypesLength; index++) {
                String acceptableMimeType = acceptableMimeTypes[index];

                /**
                 * The fileChooserParams.getAcceptTypes() API stats that the it returns an array of acceptable MIME
                 * types. The returned MIME type could be partial such as audio/* . Currently, there are plugins that
                 * return extensions when the form input type is utilized instead of
                 *  MIME types. The logic below is accommodates this use case by utilizing the extension to resolve
                 *  the appropriate
                 *  Mime type.
                 */
                if (acceptableMimeType.contains(".")) {
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

            intent.putExtra(Intent.EXTRA_MIME_TYPES, resolvedMimeTypes);
        }

        if (mOnShowFileChooserListener != null) {
            mOnShowFileChooserListener.startActivityForFileChooserResult(intent, WEB_CHROME_CLIENT_FILE_PICKER);
        }

        return true;
    }

    void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (intent != null && resultCode == RESULT_OK && requestCode == WEB_CHROME_CLIENT_FILE_PICKER) {
            Uri[] selectedUris = null;

            // if ClipData is not empty that means there are multiple files.
            if (intent.getClipData() != null) {
                // process multiple files
                int clipDataItemCount = intent.getClipData().getItemCount();
                selectedUris = new Uri[clipDataItemCount];
                for (int index = 0; index < clipDataItemCount; index++) {
                    selectedUris[index] = intent.getClipData().getItemAt(index).getUri();
                }
            } else if (intent.getData() != null) {
                // process the single file single-selected file
                selectedUris = WebChromeClient.FileChooserParams.parseResult(resultCode, intent);
            }
            mFilePathCallback.onReceiveValue(selectedUris);
        }
    }

    interface OnShowFileChooserListener {
        void startActivityForFileChooserResult(Intent intent, int requestCode);
    }
}
