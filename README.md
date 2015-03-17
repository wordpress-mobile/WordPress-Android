# MediaPicker
MediaPicker is an Android Fragment that allows capture and selection of media assets from a variety of sources.

# Features
* Select individual or multiple media items (image or video)
* Capture new media content during selection
* Configuration of which media content is presented; both media source and content type
* MediaSource interface to provide media from anywhere

## Usage
The MediaPicker library comes with two MediaSources for accessing media on device view the MediaStore.

The sample project is an example of how to configure the MediaPickerFragment to display different sets of media content (one for images, one for videos).

## Testing
Much of the functionality of the library is tested. You can run tests and generate a code coverage report by running the jacocoTestReport task.

[Robolectric][2] is used to unit test Android elements in isolation, please use this framework when writing new tests to keep them lightweight.

## Installation
The MediaPicker library is hosted on Maven Central. Example build.gradle dependency:
>dependencies {<br>&nbsp;&nbsp;&nbsp;&nbsp;compile 'org.wordpress:mediapicker:1.+'<br>}

## Author(s)
WordPress, mobile@automattic.com

## License(s)
MediaPicker is available under the GNU GPL v2 or MIT licenses.

## Apps that use this library

* [WordPress for Android][1]

[1]: https://github.com/wordpress-mobile/WordPress-Android
[2]: http://robolectric.org/
