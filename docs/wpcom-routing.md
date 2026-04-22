# WP.com API Routing

How the apps decide whether to hit a site's REST API directly or go
through the `public-api.wordpress.com` proxy. This spec covers the
decision itself, where it is made, and the edge cases that have
historically misrouted sites.

## TL;DR

Every `WpApiClient` request is routed via one of two paths:

- **Proxy** — `https://public-api.wordpress.com/wp/v2/sites/{siteId}/...`
  authenticated with the account's OAuth bearer token.
- **Direct** — `https://{site-url}/wp-json/...` authenticated with the
  site's stored WordPress application password.

The decision lives in `RsSite.shouldUseWpComProxy()`
([`RsSite.kt`](../libs/networking-rs/src/main/java/org/wordpress/android/networking/rs/RsSite.kt)),
with a fluxc-side `SiteModel.shouldUseWpComProxy()` adapter in
[`SiteModelRsExtensions.kt`](../libs/fluxc/src/main/java/org/wordpress/android/fluxc/utils/extensions/SiteModelRsExtensions.kt)
so existing fluxc call sites don't need to convert at every call.
Every routing call site goes through one of those two — there are no
other places that pick a path.

## Site types

| Site type | Flags that identify it |
| --- | --- |
| **WP.com Simple** | `isWPCom=true`, `isWPComAtomic=false` |
| **WP.com Atomic** | `isWPCom=true`, `isWPComAtomic=true`, `isJetpackConnected=true` |
| **Jetpack-REST** | `isWPCom=false`, `isJetpackConnected=true`, `origin=ORIGIN_WPCOM_REST` |
| **Jetpack-XMLRPC** | `isWPCom=false`, `isJetpackConnected=true`, `origin=ORIGIN_XMLRPC` |
| **Self-hosted** | `isWPCom=false`, `isJetpackConnected=false` |

"App password" means the site has both `apiRestUsernamePlain` and
`apiRestPasswordPlain` set to non-empty strings. Empty strings are
written by `SiteStore#removeApplicationPassword` and are treated as
"no password" — this is why both `SiteModel#hasApplicationPassword()`
and `RsSite.hasApplicationPassword` check `isEmpty()` rather than just
`!= null`.

## The decision matrix

| Site type | No app password | With app password |
| --- | --- | --- |
| WP.com Simple | **Proxy** | **Proxy** (app passwords don't apply) |
| WP.com Atomic | **Proxy** | **Direct** |
| Jetpack-REST | **Proxy** | **Direct** |
| Jetpack-XMLRPC | **Proxy** | **Direct** |
| Self-hosted | Direct (will 401 until app password set) | **Direct** |

The implementation (on the fluxc-free `RsSite` descriptor):

```kotlin
fun RsSite.shouldUseWpComProxy(): Boolean = when {
    isWPComSimpleSite -> true
    (isWPComAtomic || isJetpackConnected) && !hasApplicationPassword -> true
    else -> false
}
```

### Why each row

- **Simple**: has no publicly-reachable `/wp-json` endpoint — the only
  way to talk to it is through `public-api.wordpress.com`. App
  passwords aren't provisioned for Simple sites, so the second column
  is theoretical.
- **Atomic / Jetpack**: historically proxied (because the app has an
  OAuth token and the proxy is the "standard" WP.com path). Once the
  user provisions an app password for the site, we prefer talking
  directly — it avoids a WP.com hop, surfaces real error codes from
  the origin, and lets the app act as a first-class REST client.
- **Jetpack-XMLRPC**: the site is Jetpack-connected but its
  `origin=ORIGIN_XMLRPC` means the app hasn't yet confirmed
  REST-API-via-WP.com availability through normal signals. Routing
  REST calls through the WP.com proxy is the only way the call can
  succeed — the `/wp-json` endpoint isn't reachable. Once an app
  password is stored we can talk to the site directly.
- **Self-hosted**: no WP.com account association, so the proxy isn't
  even an option; it's direct or nothing.

## Where the decision is applied

[`WpApiClientProviderImpl`](../WordPress/src/main/java/org/wordpress/android/networking/restapi/WpApiClientProviderImpl.kt)
consumes `shouldUseWpComProxy()` in two places:

- `getWpApiClient(site)` — picks between a cached `wpComClients` entry
  (keyed by `SiteModel.siteId`) and a cached `selfHostedClients` entry
  (keyed by `SiteModel.id`, the local DB id).
- `getApiUrlResolver(site)` — returns either `WpComDotOrgApiUrlResolver`
  or `WpOrgSiteApiUrlResolver`.

`SiteModel#getWpApiRestUrl()` has a separate override for Simple sites
that returns the synthesized public-api URL. That value is copied onto
`RsSite.wpApiRestUrl` by `SiteModel.toRsSite()` and read via
`RsSite.buildRestApiUrl()` inside `createSelfHostedClient` and
`getApiRootUrlFrom`, both of which are unreachable for Simple sites
(they always take the proxy branch first), so there is no double-proxy
risk.

## Caching

- WP.com clients are cached by `RsSite.siteId` (the WP.com remote
  site id). A single `WpApiClient` can safely service every account
  for a given site because the OAuth token is resolved dynamically
  per-request via `WpDynamicAuthenticationProvider`.
- Self-hosted clients are cached by `RsSite.localId` (the local DB id
  passed through from `SiteModel.id`) because credentials are baked
  into the client at construction time. `clearSelfHostedClient(siteId)`
  is called whenever credentials change so the next request picks up
  fresh ones.
- Sign-out clears every cached client via `clearAllClients()`.

## Testing

- **[`WpApiClientRoutingTest`](../WordPress/src/test/java/org/wordpress/android/fluxc/network/rest/wpapi/rs/WpApiClientRoutingTest.kt)**
  covers the full site-type × app-password matrix plus the
  empty-credential and Atomic-without-jetpack-connected edge cases.
- **[`SiteModelSimpleAndProxyUrlTest`](../WordPress/src/test/java/org/wordpress/android/fluxc/model/site/SiteModelSimpleAndProxyUrlTest.kt)**
  covers `isWPComSimpleSite` classification and the Simple-site
  proxy-URL override inside `SiteModel#getWpApiRestUrl()`.

Both test files live in the app module, not fluxc, per the policy of
avoiding new code in the deprecated fluxc module.

## History

The original Atomic-routing bug: before `shouldUseWpComProxy()`
existed, the decision was `site.isWPCom || site.isUsingWpComRestApi`,
which forced proxy for **every** Atomic site — including ones with an
app password provisioned. Atomic + app-password sites were unable to
use any API feature that specifically required direct-to-origin
access. The current matrix fixes that by gating the proxy choice on
`!hasApplicationPassword()` for Atomic and Jetpack categories.
