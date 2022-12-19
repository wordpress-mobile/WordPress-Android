package org.wordpress.android.networking;

import android.content.Context;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.inject.Inject;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import dagger.hilt.android.testing.HiltAndroidRule;
import dagger.hilt.android.testing.HiltAndroidTest;
import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

@HiltAndroidTest
public class GravatarApiTest {
    @Rule
    public HiltAndroidRule hiltRule = new HiltAndroidRule(this);

    @Inject Context mContext;

    @Before
    public void setUp() {
        hiltRule.inject();
    }

    @Test
    public void testGravatarUploadRequest() throws IOException {
        final String fileContent = "abcdefg";

        File tempFile = new File(mContext.getCacheDir(), "tempFile.jpg");
        FileOutputStream fos = new FileOutputStream(tempFile);
        fos.write(fileContent.getBytes());
        fos.flush();
        fos.close();

        final String email = "a@b.com";
        Request uploadRequest = GravatarApi.prepareGravatarUpload(email, tempFile);

        assertEquals("POST", uploadRequest.method());

        RequestBody requestBody = uploadRequest.body();
        assertTrue(requestBody.contentType().toString().startsWith("multipart/form-data"));

        final Buffer buffer = new Buffer();
        requestBody.writeTo(buffer);
        final String body = buffer.readUtf8();

        assertTrue(body.contains("Content-Disposition: form-data; name=\"account\""));
        assertTrue(body.contains("Content-Length: " + email.length()));
        assertTrue(body.contains(email));

        assertTrue(body.contains(
                "Content-Disposition: form-data; name=\"filedata\"; filename=\"" + tempFile.getName() + "\""));
        assertTrue(body.contains("Content-Type: multipart/form-data"));
        assertTrue(body.contains(fileContent));
    }
}
