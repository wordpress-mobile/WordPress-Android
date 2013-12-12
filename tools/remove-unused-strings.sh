#!/bin/sh

unused_strings=$(lint --check  UnusedResources . \
    | grep "res/values/strings.xml" \
    | grep -o "R\.string\.[^ ]*" \
    | sed "s/R.string.//" \
    | tr "\n" "|" \
    | sed 's/|/"|"/g' \
    | sed 's/^/"/' \
    | sed 's/|"$//')

if [ "$unused_strings"x = x ]; then
    echo res/values/strings.xml is already clean
else
    grep -E -v "$unused_strings" res/values/strings.xml > tmp.xml
    mv tmp.xml res/values/strings.xml
    echo $(echo "$unused_strings" | sed "s/[^|]//g" | wc -c) strings removed
fi
