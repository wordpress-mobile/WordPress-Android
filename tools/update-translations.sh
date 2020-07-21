#!/bin/sh

SCRIPT_DIR=$(dirname "$0")
BUILD_TYPE=$1
BUILD_FILTER=$2
LANG_FILE="${SCRIPT_DIR}/../tools/exported-language-codes.csv"
RESDIR="${SCRIPT_DIR}/../WordPress/src/main/res/"

# Language definitions resource file
HEADER=\<?xml\ version=\"1.0\"\ encoding=\"UTF-8\"?\>\\n\<!--Warning:\ Auto-generated\ file,\ don\'t\ edit\ it.--\>\\n\<resources\>\\n\<string-array\ name=\"available_languages\"\ translatable=\"false\"\>
FOOTER=\\n\</string-array\>\\n\</resources\>\\n
PREPEND=\\n\<item\>
APPEND=\</item\>
LANGUAGE_DEF_FILE=$RESDIR/values/available_languages.xml
echo $HEADER > $LANGUAGE_DEF_FILE

# Filter definition
filter="current"
strings_file="strings.xml"
base_url="http://translate.wordpress.org/projects/apps/android/dev"

if [ -n "$BUILD_TYPE" ]; then 
    base_url="https://translate.wordpress.com/projects/wporg/apps/android";
    LANG_FILE="${SCRIPT_DIR}/../tools/review-language-codes.csv"
fi

if [ "$BUILD_FILTER" == "review" ]; then 
    filter=$BUILD_FILTER; 
    strings_file="strings-$filter.xml"
fi

# Inject default en_US language
echo $PREPEND >> $LANGUAGE_DEF_FILE
echo en_US >> $LANGUAGE_DEF_FILE
echo $APPEND >> $LANGUAGE_DEF_FILE

for line in $(grep -v en-rUS $LANG_FILE) ; do
    code=$(echo $line|cut -d "," -f1|tr -d " ")
    local=$(echo $line|cut -d "," -f2|tr -d " ")
    echo $PREPEND >> $LANGUAGE_DEF_FILE
    echo $local | sed s/-r/_/ >> $LANGUAGE_DEF_FILE
    echo $APPEND >> $LANGUAGE_DEF_FILE
    echo updating $local - $code
    test -d $RESDIR/values-$local/ || mkdir $RESDIR/values-$local/
    test -f $RESDIR/values-$local/$strings_file && cp $RESDIR/values-$local/$strings_file $RESDIR/values-$local/$strings_file.bak
    
    curl -sSfL --globoff "$base_url/$code/default/export-translations?filters[status]=$filter&format=android" | sed $'s/\.\.\./\…/' | sed $'s/\t/    /g' | sed -E '/Translation-Revision-Date/!s/([[:digit:]])-([[:digit:]])/\1–\2/g' > $RESDIR/values-$local/$strings_file || (echo Error downloading $code && rm -rf $RESDIR/values-$local/)
    test -f $RESDIR/values-$local/$strings_file.bak && rm $RESDIR/values-$local/$strings_file.bak
done

echo $FOOTER >> $LANGUAGE_DEF_FILE
