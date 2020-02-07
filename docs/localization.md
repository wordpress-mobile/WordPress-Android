# Localization

During development, adding a string in the [`values/strings.xml`](../WordPress/src/main/res/values/strings.xml) resource and using it in the code or layout file should be enough. 

```xml
<!-- strings.xml -->
<string name="stats_comments_posts_and_pages">Posts and pages</string>
```

```kotlin
// In code
val label = context.getString(R.string.stats_comments_posts_and_pages)
```

```xml
<!-- layout.xml -->
<TextView
    ...
    android:text="@string/stats_comments_posts_and_pages"
    ...
    />
```

You shouldn't need to touch the `strings.xml` for the other languages. During the release process, the `values/strings.xml` file is uploaded to [GlotPress](https://translate.wordpress.org/projects/apps/android/) for translation. Before the release build is finalized, all the translations are grabbed from GlotPress and saved back to their appropriate `values-[lang_code]/strings.xml` file.

## Use Meaningful Names

Meaningful names help give more context to translators. Whenever possible, the first part of the `name` should succinctly describe where the string is used. 

```xml
<!-- Do -->
<string name="stats_comments_posts_and_pages">Posts and pages</string>
```

```xml
<!-- Avoid -->
<string name="comments_posts_and_pages">Posts and pages</string>
```

If the string is for a [`contentDescription`](https://developer.android.com/reference/android/view/View.html#attr_android:contentDescription), consider adding `_content_description` to the end.

```xml
<string name="stats_expand_content_description">Expand</string>
```

## Use Placeholders Instead of Concatenation

Concatenating strings to include dynamic values splits them into separate translatable items. The completed (joined) sentence may end up not being grammatically correct, especially for RTL languages.

```xml
<!-- Don't -->
<string name="reader_discover_attribution_first_part">Originally posted by</string>
<string name="reader_discover_attribution_second_part">on</string>
```

```kotlin
// Don't
val label = context.getString(string.reader_discover_attribution_first_part) +
        " $author " + context.getString(string.reader_discover_attribution_second_part) + " $blog"
```

Use placeholders instead. They give more context and enables translators to move them where they make sense.

```xml
<!-- Do -->
<string name="reader_discover_attribution_author_and_blog">Originally posted by %1$s on %2$s</string>
```

```kotlin
// Do 
val label = String.format(
        context.getString(string.reader_discover_attribution_author_and_blog),
        author, blog
)
```

Also consider adding information about what the placeholders are in the `name`.

