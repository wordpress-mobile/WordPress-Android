package org.wordpress.android.networking;

import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.networking.gravatar.GravatarClient;
import org.wordpress.android.networking.gravatar.GravatarUploadResponse;
import org.wordpress.android.networking.gravatar.ServiceGenerator;

import java.io.File;

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

    public static Call<GravatarUploadResponse> prepareGravatarUpload(GravatarClient gravatarClient, String email,
            File file) {
        final MediaType MultiPartFormData = MediaType.parse("multipart/form-data");

        RequestBody account = RequestBody.create(MultiPartFormData, email);
        MultipartBody.Part body = MultipartBody.Part.createFormData("filedata", file.getName(), new StreamingRequest
                (MultiPartFormData, file));

        return gravatarClient.uploadImage(account, body);
    }

    public static void uploadGravatar(final File file, final GravatarUploadListener gravatarUploadListener) {
        GravatarClient client = ServiceGenerator.createService(GravatarClient.class, AccountHelper.getDefaultAccount
                ().getAccessToken(), "Bearer");

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
