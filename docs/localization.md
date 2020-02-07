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

