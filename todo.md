# Implement intent handling for opening post for editing by deeplink or weblink

[X] Separate URI parsing for deep-linked POST intents from generic deep-link intent URIs

[X] Parse blogId and postId from deep link query parameters // wordpress://post?blogId=1231&postId=24
    [X] If no parameter is given, open empty editor in current site (current default)
    [X] If post id is given without blog id, ignore both and open empty editor in current site
    [X] If blog id is given, open editor for that blog
    [X] If post id is also given, open editor with that post

[X] Parse blogId and postId from web link query parameters // https://wordpress.com/post/sitename.com/24
    [X] If no parameter is given, open empty editor in current site (current default)
    [X] If blog id is given, open editor for that blog
    [X] If post id is given without blog id, ignore both and open empty editor in current site
    [X] If post id is also given, open editor with that post

[X] Show toasts when post or site not found and a default action is taken

[ ] If remote post id not found in mPostStore, try to refresh posts from server and try again.
    [ ] If post is still not found, show toast? and open empty post


## Test

### Site & Post ids

Test site: 177737282
Post id: 37

Other test site: 177737315

### Deep link

#### Site and post id -- open post for editing, if site and post are found

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "wordpress://post?blogId=177737282\&postId=37"

#### Site name with no post id -- open new post in given site, if found

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "wordpress://post?blogId=177737282"

#### Post Id with no blog id -- open new post in current site

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "wordpress://post?postId=37"

#### Post Id with wrong blog id -- open new post in current site

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "wordpress://post?blogId=1"

#### No site name or post id -- open new post in current site

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "wordpress://post"


### Web link

#### Site name and post id -- open post for editing, if site and post are found

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "https://wordpress.com/post/testsite650835620.wordpress.com/37"

#### Site name with no post id -- open new post in given site, if found

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "https://wordpress.com/post/testsite650835620.wordpress.com/"

#### Post Id with no blog id -- open new post in current site

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "https://wordpress.com/post/37"

#### Post Id with wrong blog id -- open new post in current site

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "https://wordpress.com/post/example1231.com"

#### No site name or post id -- open new post in browser (not in app)

adb shell am start \
 -W -a android.intent.action.VIEW \
 -c android.intent.category.BROWSABLE \
 -d "https://wordpress.com/post/"

