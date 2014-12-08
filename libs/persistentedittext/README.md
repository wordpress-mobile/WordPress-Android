# PersistentEditText

PersistentEditText is a subclass of the EditText widgets that helps you saving and retrieving user entered text.

## Example Usage

You can add it as a library in your build.gradle file. PersistentEditText is hosted on the maven central repository. For instance:

    dependencies {
        // use the latest 1.x version
        compile 'org.wordpress:persistentedittext:1.+'
    }

Sample usage:

    <org.wordpress.persistentedittext.PersistentEditText
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:id="@+id/my_id"
        app:persistenceEnabled="true"/>

## Hack it

### Directory structure

	|-- PersistentEditText
	|   |-- build.gradle      # main build file
	|   `-- src
	|       |-- androidTest   # tests
	|       `-- main          # main code and resources
	|-- README.md
	|-- local.properties
	`-- settings.gradle

### Build

Create your `local.properties` file or copy it from another project, then you can generate the aar file:

	$ echo "sdk.dir=YOUR_SDK_DIR" > local.properties
    $ ./gradlew build

## Apps that use this library

* [WordPress for Android][1]

## LICENSE

This library is dual licensed unded MIT and GPL v2.

## CHANGELOG

### 1.0

* Initial release

[1]: https://github.com/wordpress-mobile/WordPress-Android
