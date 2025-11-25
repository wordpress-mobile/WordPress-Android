# Network Request Paths Research

This document tracks all network request mechanisms in the WordPress Android app for Chucker integration.

## Summary

| # | Path | Type | Chucker Compatible | Notes |
|---|------|------|-------------------|-------|
| 1 | OkHttpClientModule | OkHttp via Volley | Yes | Main network path, all FluxC clients |
| 2 | WpApiClientProvider (FluxC) | OkHttp (wordpress-rs) | Yes | Self-hosted WP sites, ~5-6 features |
| 3 | WpComApiClientProvider | OkHttp (wordpress-rs) | Yes | WordPress.com API |
| 4 | WPcomLoginClient | OkHttp (standalone) | Yes | OAuth token exchange |
| 5 | Glide | Volley (uses FluxC RequestQueue) | Yes | Image loading, already covered by #1 |
| 6 | TempAttachmentsUtil | HttpURLConnection | **No** | Video downloads for support tickets |
| 7 | WPWebViewClient | HttpURLConnection | **No** | WebView image loading with auth |

**Coverage Assessment:**
- **OkHttp paths (1-5):** Can be intercepted with Chucker
- **HttpURLConnection paths (6-7):** Cannot be intercepted by Chucker (not OkHttp-based)

---

## Detailed Findings

### 1. OkHttpClientModule (FluxC) - MAIN PATH

**File:** `libs/fluxc/src/main/java/org/wordpress/android/fluxc/module/OkHttpClientModule.java`

This is the **primary network configuration** for the app. It provides multiple OkHttpClient variants:

| Named Qualifier | Purpose |
|-----------------|---------|
| `@Named("regular")` | Default client with cookies, standard timeouts |
| `@Named("no-cookies")` | Same as regular but without cookie jar |
| `@Named("no-redirects")` | Disables automatic redirects |
| `@Named("custom-ssl")` | Custom SSL with MemorizingTrustManager |
| `@Named("custom-ssl-custom-redirects")` | Custom SSL + custom redirect handling |

**Key Architecture:**
```
OkHttpClientModule
    └── provides OkHttpClient variants
            └── used by ReleaseNetworkModule
                    └── creates RequestQueue (Volley) with OkHttpStack
                            └── used by all FluxC REST clients
```

**Interceptor injection point:**
```java
@Multibinds abstract @Named("interceptors") Set<Interceptor> interceptorSet();
@Multibinds abstract @Named("network-interceptors") Set<Interceptor> networkInterceptorSet();
```

The `@Named("regular")` client adds all interceptors from these sets. This is where Chucker should be injected.

**FluxC clients using this path:**
- `BaseWPComRestClient` - WordPress.com REST API (dozens of feature clients)
- `BaseXMLRPCClient` - XML-RPC for self-hosted sites
- `BaseWPAPIRestClient` - WP REST API (older implementation)
- And many more...

---

### 2. WpApiClientProvider (FluxC) - wordpress-rs library

**File:** `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpapi/rs/WpApiClientProvider.kt`

Uses the `wordpress-rs` Rust library (via UniFFI bindings) for WP REST API calls.

**Two methods create OkHttpClient:**

1. `getWpApiClient()` - Uses default `WpRequestExecutor` (has its own internal OkHttp)
2. `getWpApiClientCookiesNonceAuthentication()` - Creates custom OkHttpClient:
   ```kotlin
   val okHttpClient = OkHttpClient.Builder()
       .cookieJar(...)
       .build()
   ```

**Features using this path:**
- Media uploads/management
- Taxonomy management
- Application passwords
- Other WP REST API features (~5-6 total)

**Chucker integration:** Need to modify this class to accept an interceptor.

---

### 3. WpComApiClientProvider - wordpress-rs for WP.com

**File:** `WordPress/src/main/java/org/wordpress/android/networking/restapi/WpComApiClientProvider.kt`

Similar to #2, but for WordPress.com API:

```kotlin
val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
    .readTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
    .writeTimeout(READ_WRITE_TIMEOUT, TimeUnit.SECONDS)
    .build()
```

**Chucker integration:** Need to modify to accept an interceptor.

---

### 4. WPcomLoginClient - Standalone OkHttp

**File:** `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpapi/WPcomLoginClient.kt`

Creates its own bare OkHttpClient:
```kotlin
private val client = OkHttpClient()
```

Used only for OAuth token exchange (`exchangeAuthCodeForToken`).

**Chucker integration:** Need to modify to accept an OkHttpClient or interceptor.

---

### 5. Glide Image Loading

**File:** `WordPress/src/main/java/org/wordpress/android/modules/WordPressGlideModule.kt`

Glide is configured to use Volley with FluxC's RequestQueue:
```kotlin
@Inject @Named("custom-ssl-custom-redirects") lateinit var requestQueue: RequestQueue
```

Since this RequestQueue uses OkHttpStack (which uses the OkHttpClient from #1), Glide image requests **will be captured** by Chucker if we add the interceptor to OkHttpClientModule.

---

### 6. TempAttachmentsUtil - Raw HttpURLConnection (NOT INTERCEPTABLE)

**File:** `WordPress/src/main/java/org/wordpress/android/support/he/util/TempAttachmentsUtil.kt`

Uses raw `HttpURLConnection` for downloading videos:
```kotlin
connection = (URL(videoUrl).openConnection() as HttpURLConnection).apply {
    requestMethod = "GET"
    setRequestProperty("Authorization", "Bearer ${accountStore.accessToken}")
    instanceFollowRedirects = true
}
```

**Chucker cannot intercept this** - it's not OkHttp-based.

**Options:**
1. Migrate to OkHttp
2. Accept this won't be logged (it's just for support ticket attachments)

---

### 7. WPWebViewClient - Raw HttpURLConnection (NOT INTERCEPTABLE)

**File:** `WordPress/src/main/java/org/wordpress/android/util/WPWebViewClient.java`

Uses `HttpURLConnection` for loading images in WebViews with authentication:
```java
HttpURLConnection urlConnection = (HttpURLConnection) imageUrl.openConnection();
urlConnection.setRequestProperty("Authorization", "Bearer " + mToken);
```

**Chucker cannot intercept this** - it's not OkHttp-based.

**Options:**
1. Migrate to OkHttp
2. Accept this won't be logged (WebView resource loading is less critical for debugging)

---

## Dependencies Analysis

### From gradle/libs.versions.toml

| Dependency | Version | Network Usage |
|------------|---------|---------------|
| `squareup-okhttp3` | 5.3.1 | Main HTTP client |
| `squareup-retrofit` | 3.0.0 | Present but usage unclear |
| `android-volley` | 1.2.1 | Request queue, uses OkHttpStack |
| `wordpress-rs` | trunk-xxx | Rust library with UniFFI bindings, uses OkHttp |
| `bumptech-glide` | 5.0.5 | Image loading, uses Volley integration |
| `bumptech-glide-volley-integration` | 5.0.5 | Connects Glide to Volley |
| `automattic-rest` | 1.0.8 | Unknown - needs investigation |
| `apache-http-client-android` | 4.3.5.1 | Legacy, likely unused for network |

---

## Implementation Approach

### Phase 1: Core Integration (Covers ~90% of traffic)

1. **Create wrapper interceptor** that checks user preference:
   ```kotlin
   class ChuckerWrapperInterceptor @Inject constructor(
       private val chuckerInterceptor: ChuckerInterceptor,
       private val preferences: AppPrefs
   ) : Interceptor {
       override fun intercept(chain: Chain): Response {
           return if (preferences.isChuckerEnabled()) {
               chuckerInterceptor.intercept(chain)
           } else {
               chain.proceed(chain.request())
           }
       }
   }
   ```

2. **Add to OkHttpClientModule** via `@IntoSet`:
   ```kotlin
   @Provides @IntoSet @Named("interceptors")
   fun provideChuckerInterceptor(...): Interceptor = ChuckerWrapperInterceptor(...)
   ```

3. **Modify WpApiClientProvider** to accept interceptor parameter

4. **Modify WpComApiClientProvider** to accept interceptor parameter

5. **Modify WPcomLoginClient** to accept OkHttpClient or interceptor

### Phase 2: Optional (Covers remaining ~10%)

6. (Optional) Migrate `TempAttachmentsUtil` to OkHttp
7. (Optional) Migrate `WPWebViewClient` image loading to OkHttp

---

## Open Questions

1. Should we add Chucker as a regular dependency or use the no-op variant for release?
   - **Answer:** Regular dependency since it's for production use (user opt-in)

2. Where should the user preference be stored?
   - `SharedPreferences`? `AppPrefs`?

3. How should users enable Chucker?
   - Settings screen toggle?
   - Hidden developer option?
   - Deep link for support to send?

4. Should we configure header redaction for sensitive headers?
   - `Authorization`, `Cookie`, etc.

5. What retention period makes sense?
   - 1 hour? 24 hours? Unlimited until cleared?

---

*Document created: 2025-11-25*
*Status: Research Complete*
