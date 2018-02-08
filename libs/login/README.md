# WordPress-Login-Flow-Android

A pluggable WordPress login flow for Android.

## Usage ##

To use this library in your project, you must set it up as a subtree.
From the root of your main project, add the subtree:

    $ git subtree add --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git develop

This will create a new directory, `libs/login`, containing the contents of this repository.

Next, you need to generate the `gradle.properties` file:

    $ cp libs/login/gradle.properties-example libs/login/gradle.properties

Configure the fields in `gradle.properties` to match the `compileSdkVersion`,
`buildToolsVersion`, `targetSdkVersion`, and support library versions used
by your project.

At this point, you can add it to your project's root `settings.gradle`:

```groovy
include ':libs:login:WordPressLoginFlow'
```

and to your main app module's `build.gradle` dependencies:

```groovy
dependencies {

    releaseCompile (project(path:':libs:login:WordPressLoginFlow', configuration: 'release')) {
        exclude group: "com.github.wordpress-mobile.WordPress-FluxC-Android", module: "fluxc";
    }
    debugCompile (project(path:':libs:login:WordPressLoginFlow', configuration: 'debug')) {
        exclude group: "com.github.wordpress-mobile.WordPress-FluxC-Android", module: "fluxc";
    }

}
```

(The above assumes that you're also using [FluxC](https://github.com/wordpress-mobile/WordPress-FluxC-Android)
in your project, and prevents a duplicated dependency, forcing WordPress-Login-Flow-Android to use
your project's FluxC config.)

You can also force the login library to use the same FluxC version as your project by declaring the version
in a `fluxCVersion` variable in your project's root `build.gradle` (and using that same variable anywhere
else your project uses FluxC):

```groovy
ext {
    fluxCVersion = '83baae61804b65cc73a7201a7252750c76066a30'
}
```

## Contributing ##

You can fetch the latest changes made to this library into your project using:

    $ git subtree pull --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git develop --squash

And you can push your own changes upstream to `WordPress-Login-Flow-Android` using:

    $ git subtree push --prefix=libs/login git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git branch-name

Note: You can add this repository as a remote to simplify the `git subtree push`/`pull` commands:

    $ git remote add loginlib git@github.com:wordpress-mobile/WordPress-Login-Flow-Android.git

This will allow to use this form instead:

    $ git subtree pull --prefix=libs/login loginlib develop --squash

## License ##

WordPress-Login-Flow-Android is an Open Source project covered by the
[GNU General Public License version 2](LICENSE.md).
