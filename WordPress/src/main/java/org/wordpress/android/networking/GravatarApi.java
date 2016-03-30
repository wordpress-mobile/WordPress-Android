package org.wordpress.android.networking;

import com.android.volley.VolleyError;
import com.wordpress.rest.MultipartRequestBuilder;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;

public class GravatarApi {

    public interface GravatarUploadListener {
        void onSuccess();
        void onError();
    }

    public static RestRequest prepareGravatarUpload(RestClient restClient, File file, final GravatarUploadListener
            gravatarUploadListener) throws IOException {
        MultipartRequestBuilder multipartRequestBuilder = new MultipartRequestBuilder();

        if (gravatarUploadListener != null) {
            multipartRequestBuilder.setResponseListener(new RestRequest.Listener() {
                @Override
                public void onResponse(JSONObject jsonObject) {
                    if (jsonObject != null) {
                        gravatarUploadListener.onSuccess();
                    } else {
                        gravatarUploadListener.onError();
                    }
                }
            });
            multipartRequestBuilder.setResponseErrorListener(new RestRequest.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    AppLog.e(AppLog.T.API, volleyError);
                    gravatarUploadListener.onError();
                }
            });
        }

        multipartRequestBuilder.addPart("filedata", file);
        multipartRequestBuilder.addPart("account", AccountHelper.getDefaultAccount().getEmail());

        return multipartRequestBuilder.build(restClient.getAbsoluteURL("upload-image"));
    }

    public static void uploadGravatar(File file, final GravatarUploadListener gravatarUploadListener) {
        RestRequest gravatarUploadRequest;
        try {
            gravatarUploadRequest = prepareGravatarUpload(WordPress.getGravatarRestClientUtilsV1()
                    .getRestClient(), file, gravatarUploadListener);
        } catch (IOException e) {
            AppLog.e(AppLog.T.API, "Cannot make the Gravatar upload request!", e);
            gravatarUploadListener.onError();
            return;
        }

        WordPress.getGravatarRestClientUtilsV1().post(gravatarUploadRequest, null, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                AppLog.e(AppLog.T.API, volleyError);
                gravatarUploadListener.onError();
            }
        });
    }
}
