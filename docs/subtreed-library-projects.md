# Subtree'd Library Projects

A number of library dependencies are managed as separate open source projects and are git-subtree'd into the WordPress Android app source tree. Use the following command to updated (pull latest) from their respective repos:

```
$ git subtree pull --squash --prefix libs/library_name https://github.com/wordpress-mobile/WordPress-Library_Name-Android.git develop
```

and substitute the `library_name` and `Library_Name` to match the library project. As an example, for the Analytics library use 'analytics' and 'Analytics' respectively.

Similarly, issue a `subtree push` to push changes committed to the main app repo, upstream to the library repo:

```
$ git subtree push --prefix libs/library_name https://github.com/wordpress-mobile/WordPress-Library_Name-Android.git develop
```

Here are the libraries currently maintained and subtree'd:

* Analytics
* Editor
* Networking
* Stores
* Utils