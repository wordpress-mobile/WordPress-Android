#!/bin/sh

unused_strings=$(lint --check  UnusedResources . \
    | grep "res/values/strings.xml" \
    | grep -o "R\.string\.[^ ]*" \
    | sed "s/R.string.//" \
    | tr "\n" "|" \
    | sed "s/|$//")

grep -E -v "$unused_strings" res/values/strings.xml > tmp.xml
mv tmp.xml res/values/strings.xml
