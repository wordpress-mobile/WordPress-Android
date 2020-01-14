# TalkBack Guidelines

“Making applications accessible not only ensures equal access to the roughly 1 billion people in the world with disabilities but also benefits people without disabilities by allowing them to customize their experiences.” - [Google Accessibility] (https://www.google.com/accessibility/for-developers/)

### Table of Contents 

- [Governing Principles](#governing-principles)
- [Getting Started](#getting-started)
- [Guidelines](#guidelines)
   - [Basics](#basics)
	- [Simple Views](#simple-views)
	- [Complex Views](#complex-views)
- [Auditing](#auditing)
- [Further Reading](#further-reading)

##<a name="governing-principles"></a>Governing Accessibility Principles 
* Make the information on the screen as visible as possible. 
* Always design the size of controls and touch areas inclusive of all use cases.
* Provide as much support context and details for all views & actions. Images, buttons, lists and the actions they enable should have thorough, well-formatted descriptions that allow the accessibility APIs to deliver their intent seamlessly. 

	For more details visit [Make apps more accessible
](https://developer.android.com/guide/topics/ui/accessibility/apps.html).
	
## <a name="getting-started"></a>Getting Started

If you have never worked with TalkBack you can visit the TalkBack resources to get started.

## <a name="basics"></a>Basics

The accessibility framework within Android has several ways in which you can provide cues to TalkBack so it knows how it make it's announcement when a view becomes focused. 

* `contentDescription`  - Each view has this attribute that allows you to set meaningful and descriptive labels. If this is missing TalkBack might not be able to provide much value to the user. 
* `importantForAccessibility` - In cases where a view or layout might not have a need to be accessible this attribute can be set. It basically allows TalkBack to ignore views that have this property set to `yes` thus eliminating the need for a content description. 

 
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
