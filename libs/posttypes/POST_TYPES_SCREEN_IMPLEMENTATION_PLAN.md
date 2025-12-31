# Post Types Screen Implementation Plan

## Overview
Replace hardcoded post types list with real data fetched from WordPress site using WordPress-RS `PostTypeCollectionWithEditContext`.

## Current State
- **Hardcoded post types**: Only "Posts" and "Pages" are shown
- **No network fetch**: Data is static, not fetched from the actual WordPress site
- **Simple UI**: Basic list with just labels

## Target State
- **Real post types from site**: Fetch actual post types using `PostTypeCollectionWithEditContext`
- **Reactive updates**: Auto-reload when database changes via ObservableCollection
- **Better UI**: Loading states, error handling, refresh capability
- **Proper endpoint resolution**: Use `postTypeDetailsToPostEndpointType()` uniffi function (in post list screens)

---

## Implementation Steps

### 1. Update `CptPostTypeItem` Data Model
**File**: `CptPostTypesViewModel.kt`

- Add fields:
  - `name: String` (display name from API, replacing current `label`)
  - `description: String?` (post type description)
- Keep existing fields:
  - `slug: String`
  - `hierarchical: Boolean`
- Add companion factory method:
  ```kotlin
  companion object {
      fun fromEntity(entity: FullEntityPostTypeDetailsWithEditContext): CptPostTypeItem {
          val postType = entity.data
          return CptPostTypeItem(
              slug = postType.slug,
              name = postType.name,
              description = postType.description,
              hierarchical = postType.hierarchical ?: false
          )
      }
  }
  ```

### 2. Update `CptPostTypesUiState`
**File**: `CptPostTypesViewModel.kt`

Add state fields:
```kotlin
data class CptPostTypesUiState(
    val postTypes: List<CptPostTypeItem> = emptyList(),
    val isFetching: Boolean = false,
    val lastError: String? = null,  // TODO: Consider better error type
    val hasFetchedOnce: Boolean = false
)
```

### 3. Update `CptPostTypesViewModel`
**File**: `CptPostTypesViewModel.kt`

**Constructor changes**:
- Inject `WpSelfHostedServiceFactory` via Hilt

**Add properties**:
```kotlin
private var observableCollection: ObservableCollection<FullEntityPostTypeDetailsWithEditContext>? = null
private var postTypeCollection: PostTypeCollectionWithEditContext? = null
private val postTypeService: PostTypeService
```

**Add methods**:

1. **Initialize in `init` block**:
   ```kotlin
   init {
       postTypeService = serviceFactory.create(site).postTypes()
       createObservableCollection()
       loadPostTypesFromCache()
       fetch()  // Auto-fetch on init
   }
   ```

2. **`createObservableCollection()`**:
   - Create underlying collection: `postTypeService.createPostTypeCollectionWithEditContext()`
   - Create observable wrapper: `postTypeService.getObservablePostTypeCollectionWithEditContext()`
   - Set up observer to call `loadPostTypesFromCache()` on DB changes

3. **`fetch()`**:
   - Check if already fetching (guard clause)
   - Update state: `isFetching = true, lastError = null`
   - Launch coroutine in `viewModelScope`
   - Call `postTypeCollection.fetch()`
   - Update state on success: `isFetching = false, hasFetchedOnce = true`
   - Handle errors: `isFetching = false, lastError = error.message`

4. **`loadPostTypesFromCache()`**:
   - Load from observable collection
   - Map entities to `CptPostTypeItem` using factory method
   - Update `_uiState` with new list

5. **Override `onCleared()`**:
   - Close observable collection
   - Cancel viewModelScope (automatic)

**Keep existing**:
- `resolveEndpointTypeId()` method (unchanged - just returns slug for custom types)
- Navigation logic

### 4. Update `CptPostTypesScreen`
**File**: `CptPostTypesScreen.kt`

**Add to TopAppBar**:
- Refresh IconButton in `actions`:
  ```kotlin
  IconButton(
      onClick = onRefreshClick,
      enabled = !uiState.isFetching
  ) {
      if (uiState.isFetching) {
          CircularProgressIndicator(modifier = Modifier.size(24.dp))
      } else {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh")
      }
  }
  ```

**Update LazyColumn content**:
- Show empty state when `postTypes.isEmpty() && !isFetching`:
  - If `hasFetchedOnce`: "No post types found"
  - Else: "Tap refresh to load post types"
- Show error message if `lastError != null` (as banner or inline)

**Update `CptPostTypeListItem`**:
- Primary text: `postType.name` (instead of label)
- Secondary text: `postType.description` (if not null/blank, truncated with ellipsis)
- Use Material3 typography properly

**Add callback**:
- `onRefreshClick: () -> Unit` parameter

---

## Key Architecture Decisions

### Why WordPress-RS Handles These Concerns
- ✅ **Caching**: Handled by WordPress-RS database layer
- ✅ **Offline-first**: SQLite cache with reactive updates
- ✅ **Retry logic**: Not needed for prototype (manual refresh available)
- ✅ **Analytics**: Out of scope for this prototype
- ✅ **Logging**: Basic error messages sufficient for prototype

### Endpoint Resolution Pattern
The post types screen just displays and passes `slug` via navigation. The actual endpoint resolution happens in post list screens:

```kotlin
// In post list ViewModel (not in this PR):
val postTypeDetails = postTypeService.getBySlug(postTypeSlug)
val endpointType = uniffi.wp_api.postTypeDetailsToPostEndpointType(postTypeDetails)
```

### Error Handling
Currently using `String?` for errors. Consider improving with sealed class in future:
```kotlin
// TODO: Consider better error type
sealed class PostTypesError {
    data class NetworkError(val message: String) : PostTypesError()
    data class DatabaseError(val message: String) : PostTypesError()
    data object Unknown : PostTypesError()
}
```

---

## Files to Modify

1. ✏️ `libs/posttypes/src/main/java/org/wordpress/android/posttypes/CptPostTypesViewModel.kt` - Major changes
2. ✏️ `libs/posttypes/src/main/java/org/wordpress/android/posttypes/compose/CptPostTypesScreen.kt` - UI enhancements
3. 📖 `libs/posttypes/src/main/java/org/wordpress/android/posttypes/CptPostTypesActivity.kt` - No changes needed

---

## Testing Plan

### Manual Testing
1. Open post types screen
2. Verify refresh button triggers fetch
3. Verify loading indicator shows during fetch
4. Verify post types from actual site are displayed
5. Verify clicking post type navigates to post list
6. Test with site that has custom post types
7. Test error states (offline, invalid credentials)

### What's NOT Tested (Out of Scope)
- Automated unit tests
- UI tests
- Integration tests
- Accessibility testing
- Performance testing

---

## Notes

This is a prototype implementation to demonstrate WordPress-RS integration. Production-readiness improvements (proper error types, comprehensive testing, accessibility) should be addressed in future iterations.
