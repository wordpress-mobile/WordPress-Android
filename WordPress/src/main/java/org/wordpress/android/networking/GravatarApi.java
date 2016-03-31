package org.wordpress.android.networking;

import com.android.volley.VolleyError;
import com.wordpress.rest.MultipartRequestBuilder;
import com.wordpress.rest.RestClient;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.networking.gravatar.GravatarClient;
import org.wordpress.android.networking.gravatar.ServiceGenerator;
import org.wordpress.android.networking.gravatar.GravatarUploadResponse;
import org.wordpress.android.util.AppLog;

import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

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
        GravatarClient client = ServiceGenerator.createService(GravatarClient.class, AccountHelper.getDefaultAccount
                ().getAccessToken(), "Bearer");

        MediaType MultiPartFormData = MediaType.parse("multipart/form-data");

        RequestBody account = RequestBody.create(MultiPartFormData, AccountHelper.getDefaultAccount().getEmail());
        MultipartBody.Part body = MultipartBody.Part.createFormData("filedata", file.getName(), RequestBody.create
                (MultiPartFormData, file));
        client.uploadImage(account, body).enqueue(new Callback<GravatarUploadResponse>() {
            @Override
            public void onResponse(Call<GravatarUploadResponse> call, Response<GravatarUploadResponse> response) {
                if (response.isSuccessful()) {
                    gravatarUploadListener.onSuccess();
                } else {
                    gravatarUploadListener.onError();
                }
            }

            @Override
            public void onFailure(Call<GravatarUploadResponse> call, Throwable t) {
                gravatarUploadListener.onError();
            }
        });
    }
}
