package org.wordpress.android.networking;

import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.networking.gravatar.GravatarClient;
import org.wordpress.android.networking.gravatar.GravatarUploadResponse;
import org.wordpress.android.networking.gravatar.ServiceGenerator;

import java.io.File;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GravatarApi {

    public interface GravatarUploadListener {
        void onSuccess();
        void onError();
    }

    public static Call<GravatarUploadResponse> prepareGravatarUpload(GravatarClient gravatarClient, String email,
            File file) {
        return gravatarClient.uploadImage(new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("account", email)
                .addFormDataPart("filedata", file.getName(), new StreamingRequest(file))
                .build());
    }

    public static void uploadGravatar(final File file, final GravatarUploadListener gravatarUploadListener) {
        GravatarClient client = ServiceGenerator.createService(GravatarClient.class, AccountHelper.getDefaultAccount
                ().getAccessToken());

        prepareGravatarUpload(client, AccountHelper.getDefaultAccount().getEmail(), file)
                .enqueue(new Callback<GravatarUploadResponse>() {
                    @Override
                    public void onResponse(Call<GravatarUploadResponse> call, Response<GravatarUploadResponse>
                            response) {
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
