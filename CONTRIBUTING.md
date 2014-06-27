# Contributing

If you're looking to contribute to the project please keep reading, but if you want to help translate the app, jump to [Contribute to translations](#contribute-to-translations).

Here's a quick guide to create a pull request for your WordPress-Android patch:

1. Fork the github project by visiting this URL: https://github.com/wordpress-mobile/WordPress-Android/fork

2. Clone the git repository

        $ git clone git@github.com:YOUR-GITHUB-USERNAME/WordPress-Android.git

3. Create a new branch in your git repository (branched from `develop` - see [Notes about branching](#notes-about-branching) below).

        $ cd WordPress-Android/
        $ git checkout develop
        $ git checkout -b issue/123-fix-for-123 # use a better title

4. Setup your build environment (see [build instructions in our README][build-instructions]) and start hacking the project. You must follow our [code style guidelines][style], write good commit messages, comment your code and write automated tests.

5. When your patch is ready, [submit a pull request][pr]. Add some comments or screen shots to help us.

6. Wait for us to review your pull request. If something is wrong or if we want you to make some changes before the merge, we'll let you know through commit comments or pull request comments.

[build-instructions]: https://github.com/wordpress-mobile/WordPress-Android#build-instructions
[pr]: https://github.com/wordpress-mobile/WordPress-Android/compare/
[style]: https://github.com/wordpress-mobile/WordPress-Android/CODESTYLE.md

# Notes about versioning

* Version `x.y` (2.8 or 4.0 for instance) are major releases. There is no distinction between a 2.9 version or a 3.0 version, we want to avoid naming like 2.142 so the version after `x.9` (2.9) is simply `x+1.0` (3.0). A new major version is released every ~4 weeks.

* Version `x.y.z` (2.8.1 or 4.0.2 for instance) are hotfix releases. We release them only when a blocking or major bug is found in the currently released version.

# Notes about branching

We use the [git flow branching model][git-flow].

* `master` branch represents latest version released in the Google Play Store. HEAD of this branch should be equal to last tagged release.

* `develop` branch represents the cutting edge version. This is probably the one you want to fork from and base your patch on. This is the default github branch.

* Version tags. All released versions are tagged and pushed in the repository. For instance if you want to checkout the 2.5.1 version:

        $ git checkout 2.5.1

* Release branches. When a new version is going to be released, we'll branch from `develop` to `release/x.y`. This marks version x.y code freeze. Only blocking or major bug fixes will be merged to these branches. They represent beta and release candidates.

* Hotfix branches. When one or several critical issues are found on current released version, we'll branch from `tags/x.y` to `hotfix/x.y.1` (or from `tags/x.y.z` to `hotfix/x.y.z+1` if a hotfix release has already been published)

* Fix or feature branches. Proposed new features and bug fixes should live in their own branch. Use the following naming convention: if a github issue exists for this feature/bugfix, the branch will be named `issue/ISSUEID-comment` where ISSUEID is the corresponding github issue id. If a github issue doesn't exist, branch will be named `feature/comment`. These branches will be merged in:
    * `hotfix/x.y.z` if the change if a fix for a released version,
    * `release/x.y` if the change if a fix for a beta or release candidate,
    * `develop` for all other cases.

Note: `release/x.y` or `hotfix/x.y.z` will be merged back in `master` after a new version is released. A new tag will be created and pushed at the same time.

[git-flow]: http://nvie.com/posts/a-successful-git-branching-model/

# Contribute to translations

We use a tool called GlotPress to manage translations. The WordPress-Android GlotPress instance lives here: https://translate.wordpress.org/projects/android/dev. To add new translations or fix existing ones, create an account over at GlotPress and submit your changes over at the GlotPress site.
