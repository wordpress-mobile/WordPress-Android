#!/bin/sh

#lint --check  UnusedResources .

unused_strings=$(cat tmp-max/mop.txt \
    | grep "res/values/strings.xml" \
    | grep -o "R\.string\.[^ ]*" \
    | sed "s/R.string.//" \
    | tr "\n" "|" \
    | sed "s/|/\\\|/g" \
    | sed "s/\\\|$//")

grep -v "$unused_strings" res/values/strings.xml > tmp.xml
mv tmp.xml res/values/strings.xml
