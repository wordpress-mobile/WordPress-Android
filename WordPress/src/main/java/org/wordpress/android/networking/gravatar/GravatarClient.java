package org.wordpress.android.networking.gravatar;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface GravatarClient {
    @POST("upload-image")
    Call<GravatarUploadResponse> uploadImage(@Body RequestBody uploadForm);
}