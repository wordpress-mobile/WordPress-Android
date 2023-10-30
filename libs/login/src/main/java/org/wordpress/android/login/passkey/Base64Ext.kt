package org.wordpress.android.login.passkey

private val BASE64_FLAG = android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE

fun ByteArray.toBase64(): String {
    return android.util.Base64.encodeToString(this, BASE64_FLAG)
}

fun String.decodeBase64(): ByteArray {
    return android.util.Base64.decode(this, BASE64_FLAG)
}