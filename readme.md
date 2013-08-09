# WordPress for Android #

If you're just looking to install WordPress for Android, you can find it on
[Google Play][].  If you're a developer wanting to contribute, read on.

[Google Play]: https://play.google.com/store/apps/details?id=org.wordpress.android


## Helpful Resources ##

Developer Blog: <http://dev.android.wordpress.org>
Source Code: <http://android.svn.wordpress.org>
Issue Tracker and Code Browser: <http://android.trac.wordpress.org/>
WordPress Mobile Developer Handbook: <http://make.wordpress.org/mobile/handbook/>



## Build Instructions ##

The full build instructions (including instructions for using Eclipse) can be
found on the [Tutorials &amp; Guides][] page of the mobile developer handbook.
Instructions for building on the command line with ant can be found below.

[Tutorials &amp; Guides]: http://make.wordpress.org/mobile/handbook/pathways/android/tutorials-guides/


### Building with Ant ###

Once you have the Android SDK installed along with the library dependencies,
run the following command from the root directory of the WordPress for Android
project:

    android update project -p .

This will create a `local.properties` file that is specific for your setup.
You can then build the project by running:

    ant debug

You can install the package onto a connected device or a virtual device by
running:

    ant installd

Also see the full Android documentation, [Building and Running from the Command
Line][command-line].

[command-line]: http://developer.android.com/tools/building/building-cmdline.html

## Run Unittests ##

    cd tests
    ant debug && ant installd && ant test
