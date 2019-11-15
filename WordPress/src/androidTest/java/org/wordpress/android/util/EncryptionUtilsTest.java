package org.wordpress.android.util;

import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.libsodium.jni.NaCl;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(AndroidJUnit4.class)
public class EncryptionUtilsTest {
    byte[] mPublicKey;
    byte[] mSecretKey;

    static final int BOX_PUBLIC_KEY_BYTES = NaCl.sodium().crypto_box_publickeybytes();
    static final int BOX_SECRET_KEY_BYTES = NaCl.sodium().crypto_box_secretkeybytes();

    static final int BASE64_DECODE_FLAGS = Base64.DEFAULT;

    // test data
    static final String TEST_EMPTY_STRING = "";
    static final String TEST_LOG_STRING = "WordPress - 13.5 - Version code: 789\n"
            + "Android device name: Google Android SDK built for x86\n\n"
            + "01 - [Nov-11 03:04 UTILS] WordPress.onCreate\n"
            + "02 - [Nov-11 03:04 API] Dispatching action: ListAction-REMOVE_EXPIRED_LISTS\n"
            + "03 - [Nov-11 03:04 API] QuickStartStore onRegister\n"
            + "04 - [Nov-11 03:04 STATS] 🔵 Tracked: deep_link_not_default_handler, "
            + "Properties: {\"interceptor_classname\":\"com.google.android.setupwizard.util.WebDialogActivity\"}\n"
            + "05 - [Nov-11 03:04 UTILS] App comes from background\n"
            + "06 - [Nov-11 03:04 STATS] 🔵 Tracked: application_opened\n"
            + "07 - [Nov-11 03:04 READER] notifications update job service > job scheduled\n"
            + "08 - [Nov-11 03:04 API] Dispatching action: SiteAction-FETCH_SITES\n"
            + "09 - [Nov-11 03:04 API] StackTrace: com.android.volley.AuthFailureError\n"
            + "    at com.android.volley.toolbox.BasicNetwork.performRequest(BasicNetwork.java:195)\n"
            + "    at com.android.volley.NetworkDispatcher.processRequest(NetworkDispatcher.java:131)\n"
            + "    at com.android.volley.NetworkDispatcher.processRequest(NetworkDispatcher.java:111)\n"
            + "    at com.android.volley.NetworkDispatcher.run(NetworkDispatcher.java:90)\n";
    static final String TEST_CHAR_SAMPLE = "!\"#$%&' ()*+,- ./{|}~[\\]^_`: ;<=>?Ⓟ @︼︽︾⑳₡\n"
            + "¢£¤¥¦§¨©ª«¬®¯ °±²ɇɈɉɊɋɌɎɏɐɑɒɓɔ ɕɖɗɘəɚ⤚▓⤜⤝⤞⤟ⰙⰚⰛⰜ⭑⬤⭒‰ ꕢ ꕣꕤ ꕥ￥￦ \n"
            + "❌ ⛱⛲⛳⛰⛴⛵ ⚡⏰⏱⏲⭐ ✋☕⛩⛺⛪✨ ⚽ ⛄⏳\n"
            + " ḛḜḝḞṶṷṸẂ ẃ ẄẅẆ ᾃᾄᾅ ᾆ Ṥṥ  ȊȋȌ ȍ Ȏȏ ȐṦṧåæçèéêë ì í ΔƟΘ\n"
            + "㥯㥰㥱㥲㥳㥴㥵 㥶㥷㥸㥹㥺 俋 俌 俍 俎 俏 俐 俑 俒 俓㞢㞣㞤㞥㞦㞧㞨쨜 쨝쨠쨦걵걷 걸걹걺ﾓﾔﾕ ﾖﾗﾘﾙ\n"
            + " ﵑﵓﵔ ﵕﵗ ﵘ  ﯿ ﰀﰁﰂ ﰃ ﮁﮂﮃﮄﮅᎹᏪ Ⴥჭᡴᠦᡀ\n";

    @Before
    public void setup() {
        mPublicKey = new byte[BOX_PUBLIC_KEY_BYTES];
        mSecretKey = new byte[BOX_SECRET_KEY_BYTES];
        NaCl.sodium().crypto_box_keypair(mPublicKey, mSecretKey);
    }

    @Test
    public void testEmptyStringEncryptionResultIsValid() {
        testEncryption(TEST_EMPTY_STRING);
    }

    @Test
    public void testLogStringEncryptionResultIsValid() {
        testEncryption(TEST_LOG_STRING);
    }

    @Test
    public void testCharacterSampleEncryptionResultIsValid() {
        testEncryption(TEST_CHAR_SAMPLE);
    }

    private void testEncryption(final String testString) {
        final JSONObject encryptionDataJson = getEncryptionDataJson(mPublicKey, testString);
        assertNotNull(encryptionDataJson);

        /*
            Expected Contents for JSON:
            {
            "keyedWith": "v1",
            "encryptedKey": "$key_as_base_64",  // The encrypted AES key
            "header": "base_64_encoded_header", // The xchacha20poly1305 stream header
            "messages": []                      // the stream elements, base-64 encoded
            }
        */

        final byte[] dataSpecificKey = getDataSpecificKey(encryptionDataJson);
        assertNotNull(dataSpecificKey);

        final byte[] header = getHeader(encryptionDataJson);
        assertNotNull(header);

        final byte[] state = new byte[EncryptionUtils.XCHACHA20POLY1305_STATEBYTES];
        final int initPullReturnCode = NaCl.sodium().crypto_secretstream_xchacha20poly1305_init_pull(
                state,
                header,
                dataSpecificKey);
        assertEquals(initPullReturnCode, 0);

        String decryptedDataString = "";
        final byte[][] encryptedLines = getEncryptedLines(encryptionDataJson);
        assertNotNull(encryptedLines);
        for (int i = 0; i < encryptedLines.length; ++i) {
            final String decryptedLine = getDecryptedString(state, encryptedLines[i]);
            if (decryptedLine == null) {
                // expecting null for the final line in the encryption data
                assertEquals(encryptedLines.length - 1, i);
                break;
            }

            decryptedDataString = decryptedDataString + decryptedLine;
        }

        assertEquals(testString, decryptedDataString);
    }
    private JSONObject getEncryptionDataJson(final byte[] publicKey, final String data) {
        try {
            final String encryptionDataJsonString = EncryptionUtils.encryptStringData(
                    Base64.encodeToString(publicKey, Base64.DEFAULT),
                    data);

            return new JSONObject(encryptionDataJsonString);
        } catch (JSONException e) {
            fail("encryptStringData failed with JSONException: " + e.toString());
        }
        return null;
    }

    private byte[] getDataSpecificKey(final JSONObject encryptionDataJson) {
        try {
            final byte[] decryptedKey = new byte[EncryptionUtils.XCHACHA20POLY1305_KEYBYTES];
            final String encryptedKeyBase64 = encryptionDataJson.getString("encryptedKey");
            final byte[] encryptedKey = Base64.decode(encryptedKeyBase64, BASE64_DECODE_FLAGS);
            final int returnCode = NaCl.sodium().crypto_box_seal_open(
                    decryptedKey,
                    encryptedKey,
                    EncryptionUtils.XCHACHA20POLY1305_KEYBYTES + EncryptionUtils.BOX_SEALBYTES,
                    mPublicKey,
                    mSecretKey);
            assertEquals(returnCode, 0);

            return decryptedKey;
        } catch (JSONException e) {
            fail("failed to get encryptedKey from encrypted data JSON");
        }

        return null;
    }

    private byte[] getHeader(final JSONObject encryptionDataJson) {
        try {
            final String headerBase64 = encryptionDataJson.getString("header");
            return Base64.decode(headerBase64, BASE64_DECODE_FLAGS);
        } catch (JSONException e) {
            fail("failed to get header from encrypted data JSON");
        }
        return null;
    }

    private byte[][] getEncryptedLines(final JSONObject encryptionDataJson) {
        try {
            final JSONArray messages = encryptionDataJson.getJSONArray("messages");

            final int messagesLength = messages.length();
            final byte[][] encryptedLines = new byte[messagesLength][];
            for (int i = 0; i < messagesLength; ++i) {
                final String messageBase64 = messages.getString(i);
                encryptedLines[i] = Base64.decode(messageBase64, BASE64_DECODE_FLAGS);
            }
            return encryptedLines;
        } catch (JSONException e) {
            fail("failed to get messages from encrypted data JSON");
        }

        return null;
    }

    private String getDecryptedString(final byte[] state, final byte[] encryptedLine) {
        final byte[] tag = new byte[1];
        final int decryptedLineLength = encryptedLine.length - EncryptionUtils.XCHACHA20POLY1305_ABYTES;
        final byte[] decryptedLine = new byte[decryptedLineLength];
        final byte[] additionalData = new byte[0]; // opting not to use this value
        final int additionalDataLength = 0;
        final int[] decryptedLineLengthOutput = new int[0]; // opting not to get this value
        final int returnCode = NaCl.sodium().crypto_secretstream_xchacha20poly1305_pull(
                state,
                decryptedLine,
                decryptedLineLengthOutput,
                tag,
                encryptedLine,
                encryptedLine.length,
                additionalData,
                additionalDataLength);
        assertEquals(returnCode, 0);

        final int encryptionTag = tag[0];
        if (encryptionTag == EncryptionUtils.XCHACHA20POLY1305_TAG_MESSAGE) {
            return new String(decryptedLine);
        } else if (encryptionTag == EncryptionUtils.XCHACHA20POLY1305_TAG_FINAL) {
            return null;
        }

        fail("message decryption failed, unexpected tag.");
        return null;
    }
}
