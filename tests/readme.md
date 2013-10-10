# WordPress Android - Test Project #

## Run tests ##

    $ ./gradlew cIT

## Dump a test database ##

    $ adb shell su -c "echo .dump | sqlite3 /data/data/org.wordpress.android/databases/wordpress"
