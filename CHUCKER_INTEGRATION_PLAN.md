# Chucker Integration Plan

## Overview

Integrate [Chucker](https://github.com/ChuckerTeam/chucker) into the WordPress Android app for comprehensive HTTP traffic inspection to help users troubleshoot issues.

## Goals

- **Full coverage**: Intercept ALL network requests made by the app
- **Production-ready**: Include in production builds, but **disabled by default**
- **User opt-in**: Users can enable when troubleshooting (e.g., when contacting support)
- **Privacy-focused**: Logs never leave the device unless user explicitly sends them
- **Dependency coverage**: Include network requests from library dependencies

## Investigation Tasks

### 1. Understand Chucker
- [ ] Research Chucker library capabilities
- [ ] Understand integration requirements (OkHttp interceptor)
- [ ] Determine how to enable/disable at runtime (user opt-in mechanism)
- [ ] Review data retention and export options for support scenarios

### 2. Identify All Network Request Paths

Known paths to investigate:

| Path | File | Status |
|------|------|--------|
| OkHttpClientModule | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/module/OkHttpClientModule.java` | To investigate |
| WpApiClientProvider | `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpapi/rs/WpApiClientProvider.kt` | To investigate |
| Legacy Volley (?) | Unknown | To investigate |
| Built-in HTTP (?) | Unknown | To investigate |

### 3. Dependency Analysis

Review `gradle/libs.versions.toml` for:
- [ ] OkHttp usages
- [ ] Retrofit usages
- [ ] Volley usages
- [ ] Other HTTP client libraries
- [ ] Built-in `HttpURLConnection` or similar

## Key Files

- `libs/fluxc/src/main/java/org/wordpress/android/fluxc/module/OkHttpClientModule.java` - Main OkHttp configuration
- `libs/fluxc/src/main/java/org/wordpress/android/fluxc/network/rest/wpapi/rs/WpApiClientProvider.kt` - WP API client (newer, ~5-6 features)
- `gradle/libs.versions.toml` - Dependency catalog

## Notes

- OkHttpClientModule covers the biggest portion of network requests
- WpApiClientProvider is newer and covers 5-6 different features
- There may be an older Volley-based implementation predating OkHttpClientModule
- Need to check for any direct `HttpURLConnection` usage

## Investigation Findings

### Chucker Research

**What is Chucker?**
- An OkHttp interceptor that captures and displays HTTP(S) traffic
- Persists network events locally on-device
- Provides an in-app UI for inspecting requests/responses
- Shows a notification with network activity summary (tapping opens the UI)
- Supports API level 21+, OkHttp 4 compatible

**Key Features:**
- Search & highlighting in body text
- Image display in responses
- Custom body decoders (Protobuf, Thrift, etc.)
- Multi-window support (Android 7+)
- Header redaction for sensitive data (e.g., `Authorization`, `Cookie`)
- Configurable retention period (e.g., 1 hour)
- Configurable max content length (~250KB default)

**Integration:**
```kotlin
val chuckerCollector = ChuckerCollector(
    context = context,
    showNotification = true,
    retentionPeriod = RetentionManager.Period.ONE_HOUR
)

val chuckerInterceptor = ChuckerInterceptor.Builder(context)
    .collector(chuckerCollector)
    .maxContentLength(250_000L)
    .redactHeaders("Authorization", "Cookie")
    .build()

val client = OkHttpClient.Builder()
    .addInterceptor(chuckerInterceptor)
    .build()
```

**⚠️ Runtime Enable/Disable Challenge:**
Chucker does **NOT** have a built-in runtime enable/disable toggle. Once the interceptor is added to OkHttpClient, it's active. Available skip mechanisms:
- `skipPaths(vararg paths: String)` - skip specific URL paths
- `skipPaths(paths: Regex)` - skip paths matching regex
- `skipDomains(vararg domains: String)` - skip specific domains
- `skipDomains(domains: Regex)` - skip domains matching regex

**Possible Solutions for Runtime Control:**
1. **Wrapper interceptor**: Create our own interceptor that wraps `ChuckerInterceptor` and checks a preference before delegating
2. **Dynamic skip pattern**: Use a regex pattern that matches everything when disabled (e.g., `.*`) - though this may still have overhead
3. **OkHttpClient recreation**: Rebuild the OkHttpClient when user toggles (expensive, may cause issues with in-flight requests)

**Recommendation:** Option 1 (wrapper interceptor) is cleanest - minimal overhead when disabled, no side effects.

**Data & Privacy:**
- Data stored locally only
- Can redact sensitive headers
- UI allows sharing/exporting captured data (user-initiated)
- Requires `POST_NOTIFICATIONS` permission on Android 13+ for notification

### Network Paths Discovered
*(To be filled in after investigation)*

### Implementation Approach
*(To be determined after full investigation)*

---

*Document created: 2025-11-25*
*Status: Planning Phase*
