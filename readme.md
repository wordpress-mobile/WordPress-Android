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

### Android SDK ###

You will, of course, first need the [Android SDK][android-sdk] for your
platform.  Once you have that installed, set an environment variable named
`ANDROID_HOME` that is set to the location of the Android SDK.  The exact path
may be a little different, but on OS X that may look something like:

    export ANDROID_HOME="/Applications/adt-bundle-mac/sdk"

Using the [Android SDK Manager][sdk-manager], install the SDK Platform for API
Level 16.

[android-sdk]: http://developer.android.com/sdk/index.html
[sdk-manager]: http://developer.android.com/tools/help/sdk-manager.html


### Maven ###

WordPress for Android uses [maven][] to manage dependencies on third party
libraries.  Most modern Linux and OS X systems will have maven preinstalled.
If not, follow the instructions on the [Maven website][maven].

After downloading the WordPress for Android source code, you should be able to
issue the following command to build the app:

    mvn clean package

If this is the first time you've run maven, this may take a while, as it
downloads all of the various dependencies.  Don't worry, future builds go much
faster.

Once the package is built, you can install it on your connected Android device
or virtual device by running:

    mvn android:deploy

Additional useful mvn commands can be found on the maven-android-plugin [Tips
and Tricks][] page, or simply by running:

    mvn android:help

[maven]: http://maven.apache.org/
[Tips and Tricks]: http://code.google.com/p/maven-android-plugin/wiki/TipsAndTricks


### Eclipse ###

To develop using Eclipse, you will need the [m2e-android plugin][] in addition
to the standard [Android Development Tools][adt].

Some project dependencies (notably, Android library projects) aren't able to be
fully included by maven, and will therefore need to be setup individually.  For
each of the following libraries, follow their instructions for downloading and
importing the project into Eclipse.

 - [ActionBarSherlock](http://actionbarsherlock.com/)

[m2e-android plugin]: http://rgladwell.github.com/m2e-android/
[adt]: http://developer.android.com/tools/sdk/eclipse-adt.html

