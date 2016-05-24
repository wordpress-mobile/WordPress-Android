package org.wordpress.android.networking;

import org.wordpress.android.models.AccountHelper;

import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Interceptor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GravatarApi {
    public static final String API_BASE_URL = "https://api.gravatar.com/v1/";

    public interface GravatarUploadListener {
        void onSuccess();
        void onError();
    }

    private static OkHttpClient createClient(final String restEndpointUrl) {
        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();

        //// uncomment the following line to add logcat logging
        //httpClientBuilder.addInterceptor(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY));

        // add oAuth token usage
        httpClientBuilder.addInterceptor(new Interceptor() {
            @Override
            public Response intercept(Interceptor.Chain chain) throws IOException {
                Request original = chain.request();

                String siteId = AuthenticatorRequest.extractSiteIdFromUrl(restEndpointUrl, original.url()
                        .toString());
                String token = OAuthAuthenticator.getAccessToken(siteId);

                Request.Builder requestBuilder = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .method(original.method(), original.body());

                Request request = requestBuilder.build();
                return chain.proceed(request);
            }
        });

        return httpClientBuilder.build();
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

        createClient(API_BASE_URL).newCall(request).enqueue(
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
