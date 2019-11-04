# Accessibility Guidelines

### Activity titles
When an Activity comes to the foreground, TalkBack announces it’s title. When the activity has no title, TalkBack announces the name of the application which might confuse the user -> **_set a title to all visible activities_**, either in AndroidManifest or using Activity.setTitle() method.

### Images
Set contentDescription attribute to all ImageViews (null is a valid value).

Illustrative images and images with labels should have contentDescription set to null -> “android:contentDescription="@null" or have importantForAccessibility set to “no” -> “android:importantForAccessibility="no".

<img src="images/accessibility-guidelines/illustrative-img.png" width="300">

ImageButtons with labels should have contentDescription set to null. Setting importanceForAccessibility to “no” makes them unfocusable in the accessibility mode.

<img src="images/accessibility-guidelines/image-with-label.png" width="300">

### Labels
When a UI element is just a label for another element, set the `labelFor` attribute.

<img src="images/accessibility-guidelines/label-for.png" width="300">

### Grouping content
If users should treat a set of elements as a single unit of information, you can group these elements in a focusable container (use android:focusable=”true”). 

<img src="images/accessibility-guidelines/grouping-content.png" width="300">

### Custom Views
Make sure that custom views are accessible with both Switch Access and TalkBack. Consider implementing accessibility functionality for them using ExploreByTouchHelper.
