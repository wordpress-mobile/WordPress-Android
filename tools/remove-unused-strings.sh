#!/bin/sh

RESDIR=WordPress/src/main/res/

unused_strings=$(lint --check  UnusedResources . \
    | grep "$RESDIR/values/strings.xml" \
    | grep -o "R\.string\.[^ ]*" \
    | sed "s/R.string.//" \
    | tr "\n" "|" \
    | sed 's/|/"|"/g' \
    | sed 's/^/"/' \
    | sed 's/|"$//')

if [ "$unused_strings"x = x ]; then
    echo $RESDIR/values/strings.xml is already clean
else
    grep -E -v "$unused_strings" $RESDIR/values/strings.xml > tmp.xml
    mv tmp.xml $RESDIR/values/strings.xml
    echo $(echo "$unused_strings" | sed "s/[^|]//g" | wc -c) strings removed
fi
