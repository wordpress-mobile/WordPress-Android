package org.wordpress.android.networking;

import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.networking.gravatar.ServiceGenerator;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.Response;

public class GravatarApi {
    public static final String API_BASE_URL = "https://api.gravatar.com/v1/";

    public interface GravatarUploadListener {
        void onSuccess();
        void onError();
    }

    public static Request prepareGravatarUpload(String email, File file) {
        return new Request.Builder()
                .url(API_BASE_URL + "upload-image")
                .post(new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("account", email)
                        .addFormDataPart("filedata", file.getName(), new StreamingRequest(file))
                        .build())
                .build();
    }

    public static void uploadGravatar(final File file, final GravatarUploadListener gravatarUploadListener) {
        Request request = prepareGravatarUpload(AccountHelper.getDefaultAccount().getEmail(), file);

        ServiceGenerator.createClient(AccountHelper.getDefaultAccount().getAccessToken()).newCall(request).enqueue(
                new Callback() {
                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (response.isSuccessful()) {
                                    gravatarUploadListener.onSuccess();
                                } else {
                                    gravatarUploadListener.onError();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(okhttp3.Call call, IOException e) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                gravatarUploadListener.onError();
                            }
                        });
                    }
                });
    }
}
