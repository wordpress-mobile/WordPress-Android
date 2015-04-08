#!/bin/sh

LANG_FILE=tools/exported-language-codes.csv
RESDIR=WordPress/src/main/res/

# Language definitions resource file
HEADER=\<?xml\ version=\"1.0\"\ encoding=\"UTF-8\"?\>\\n\<resources\>\\n\<string-array\ name=\"available_languages\"\ translatable=\"false\"\>
FOOTER=\\n\</string-array\>\\n\</resources\>\\n
PREPEND=\\n\<item\>
APPEND=\</item\>
LANGUAGE_DEF_FILE=$RESDIR/values/available_languages.xml
echo $HEADER > $LANGUAGE_DEF_FILE

for line in $(cat $LANG_FILE) ; do
    code=$(echo $line|cut -d "," -f1|tr -d " ")
    local=$(echo $line|cut -d "," -f2|tr -d " ")
    echo $PREPEND >> $LANGUAGE_DEF_FILE
    echo $code >> $LANGUAGE_DEF_FILE
    echo $APPEND >> $LANGUAGE_DEF_FILE
    echo updating $local - $code
    test -d $RESDIR/values-$local/ || mkdir $RESDIR/values-$local/
    test -f $RESDIR/values-$local/strings.xml && cp $RESDIR/values-$local/strings.xml $RESDIR/values-$local/strings.xml.bak
    curl -sSfL --globoff -o $RESDIR/values-$local/strings.xml "http://translate.wordpress.org/projects/android/dev/$code/default/export-translations?filters[status]=current&format=android" || (echo Error downloading $code && rm -rf $RESDIR/values-$local/)
    test -f $RESDIR/values-$local/strings.xml.bak && rm $RESDIR/values-$local/strings.xml.bak
done

echo $FOOTER >> $LANGUAGE_DEF_FILE
