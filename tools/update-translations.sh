#!/bin/sh

LANG_FILE=tools/exported-language-codes.csv

for line in $(cat $LANG_FILE) ; do
    code=$(echo $line|cut -d "," -f1|tr -d " ")
    local=$(echo $line|cut -d "," -f2|tr -d " ")
    echo updating $local - $code
    test -d res/values-$local/ || mkdir res/values-$local/
    test -f res/values-$local/strings.xml && cp res/values-$local/strings.xml res/values-$local/strings.xml.bak
    curl -sSf --globoff -o res/values-$local/strings.xml "http://translate.wordpress.org/projects/android/dev/$code/default/export-translations?filters[status]=current&format=android" || (echo Error downloading $code && rm -rf res/values-$local/)
    test -f res/values-$local/strings.xml.bak && rm res/values-$local/strings.xml.bak
done
