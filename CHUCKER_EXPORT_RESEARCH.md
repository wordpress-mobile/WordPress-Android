# Chucker Export Functionality Research

## Overview

This document outlines options for programmatically exporting Chucker logs so users can attach them to support tickets.

## Chucker's Export API

`ChuckerCollector` provides a `writeTransactions` method:

```kotlin
public fun writeTransactions(
    context: Context,
    startTimestamp: Long?,  // null = all transactions
    exportFormat: ExportFormat = ExportFormat.LOG,  // LOG (txt) or HAR
): Uri?  // Returns file Uri or null if failed
```

### Export Formats

- **LOG (txt)**: Human-readable text format
- **HAR**: HTTP Archive format (JSON-based, can be imported into browser dev tools)

### Notes

- This method is **blocking** and performs disk I/O - must run on background thread
- Returns a `Uri` to the exported file, or `null` if export failed
- Data is stored in Chucker's internal Room database

## Current Challenge

Our `ChuckerCollector` is encapsulated inside `TrackNetworkRequestsInterceptor`. To expose export functionality, we need to provide access to the collector or its export method.

## Implementation Options

### Option 1: Add Export Method to Interceptor

Add a method on `TrackNetworkRequestsInterceptor` that delegates to the collector:

```kotlin
fun exportTransactions(context: Context, format: ExportFormat = ExportFormat.LOG): Uri? {
    return chuckerInterceptor?.let {
        // Need access to collector - would require storing reference
    }
}
```

**Challenge**: We don't currently store a reference to the collector separately.

### Option 2: Store Collector Reference

Modify interceptor to keep a reference to the collector:

```kotlin
class TrackNetworkRequestsInterceptor(...) {
    private var collector: ChuckerCollector? = null

    private fun createChuckerInterceptor(retention: NetworkRequestsRetentionPeriod): ChuckerInterceptor {
        collector = ChuckerCollector(...)
        return ChuckerInterceptor.Builder(context)
            .collector(collector!!)
            ...
    }

    fun exportTransactions(context: Context, format: ExportFormat): Uri? {
        return collector?.writeTransactions(context, null, format)
    }
}
```

### Option 3: Separate Export Helper

Create a separate class that accesses Chucker's internal database directly. However, Chucker's database is internal and not part of the public API - this would be fragile.

### Option 4: Use Chucker's Built-in Share

Chucker's UI has a share button (visible in toolbar). Users can manually:
1. Open "View Network Requests"
2. Tap share icon
3. Share via email/files/etc.

This is already available without any code changes.

## UI Integration Options

If we implement programmatic export:

1. **Help Screen** - Add "Export Network Logs" button near "View Network Requests"
2. **Support Ticket Flow** - Auto-attach logs when creating a support ticket (with user consent)
3. **Share Intent** - Launch share sheet with exported file

## Questions to Discuss

1. **Where should export be triggered?** (Help screen, support flow, both?)
2. **Should logs be auto-attached to support tickets?** (with consent dialog)
3. **Which format?** LOG is human-readable, HAR is more detailed but harder to read
4. **Should we filter by time range?** (e.g., last hour, last session, all)
5. **Privacy review needed?** Even with redacted headers, request/response bodies may contain sensitive data

## Recommendation

For v1, Option 4 (Chucker's built-in share) may be sufficient. Users can manually share from the Chucker UI. If we need tighter integration with support tickets, Option 2 would be the cleanest implementation.

---

*Created: 2025-11-25*
