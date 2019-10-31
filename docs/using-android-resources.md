# Using Android Resources

## String Resources

We use `values/strings.xml` file for *ALL* translatable strings including string arrays. Each element in a string array should be defined as separate string resource first and then the string array should be defined with `translatable="false"` flag. This is due to a GlotPress limitation where translating arrays directly could generate smaller arrays if some elements are not translated. Here is a basic example:

```
<string name="element1">Element 1</string>
<string name="element2">Element 2</string>
<string-array name="elements_array" translatable="false">
    <item>@string/element1</item>
    <item>@string/element2</item>
</string-array>
```

We also have string resources outside of `strings.xml` such as `key_strings`. These strings are not user-facing and should be used as static strings such as preference keys.

To help ease the translation process we ask that you mark alias string resources - as well as other strings where appropriate - as not translatable. For example `<string name="foo" translatable="false">@string/bar</string>`

## Drawable Resources

Adding a vector drawable (to `WordPress/src/main/res/drawable/`) should be the first option when adding assets. Only if a vector drawable is not available should PNG files be added to the project. Make sure to use `android:src` in place of `app:srcCompat` in XML files. Use existing white 24dp variations of vector drawables (i.e. `ic_*_white_24dp`) and tint the drawables statically (i.e. XML) or dynamically (i.e. Java or Kotlin) as necessary. Set values for `android:height` and `android:width` attributes for views with icons to scale the 24dp icon for that view.

Some vector drawables may come from a SVG file and they are not the easiest file type to edit. If the SVG file is specific to the WPAndroid project (like a banner image or unlike a gridicon), then add the SVG source in `WordPress/src/future/svg/`. This will make sure we can find and edit the SVG file and then export it in vector drawable format.

Please use the following naming convention for drawables:

* Use `ic_` for icons (i.e. simple, usually single color, usually square shape) and `img_` for images (i.e. complex, usually multiple colors).
* Use the [gridicon](http://automattic.github.io/gridicons/) name if applicable (examples: `ic_my_sites` or `ic_reply`).
* Use the color of the icon (example: `ic_reply_white`).
* Use the width in dp (example: `ic_reply_white_24dp`).

#### Valid
`ic_reply_white_24dp` (white reply icon 24dp)  
`ic_stats_black_32dp` (black stats icon 32dp)
#### Invalid
`reply_white` (missing `ic_` and width)  
`ic_confetti_284dp` (uses `ic_`, but should use `img_`)  
`img_confetti_98dp` (uses height, but should use width)