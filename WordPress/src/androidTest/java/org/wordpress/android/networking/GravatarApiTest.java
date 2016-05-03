package org.wordpress.android.networking;

import android.test.InstrumentationTestCase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.Request;
import okhttp3.RequestBody;
import okio.Buffer;

public class GravatarApiTest extends InstrumentationTestCase {

    public void testGravatarUploadRequest() throws IOException {
        final String fileContent = "abcdefg";

        File tempFile = new File(getInstrumentation().getTargetContext().getCacheDir(), "tempFile.jpg");
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

        assertTrue(body.contains("Content-Disposition: form-data; name=\"filedata\"; filename=\"" + tempFile.getName() + "\""));
        assertTrue(body.contains("Content-Type: multipart/form-data"));
        assertTrue(body.contains(fileContent));
    }
}
