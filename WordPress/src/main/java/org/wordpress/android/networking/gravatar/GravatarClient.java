package org.wordpress.android.networking.gravatar;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface GravatarClient {
    @Multipart
    @POST("upload-image")
    Call<GravatarUploadResponse> uploadImage(@Part("account") RequestBody account, @Part MultipartBody.Part image);
}