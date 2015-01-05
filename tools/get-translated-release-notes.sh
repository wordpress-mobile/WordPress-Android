#!/bin/sh

LANG_FILE=tools/release-notes-language-codes.csv
RESDIR=tools/release-notes

function fetch() {
	for line in $(cat $LANG_FILE) ; do
		code=$(echo $line|cut -d "," -f1|tr -d " ")
		local=$(echo $line|cut -d "," -f2|tr -d " ")
		echo updating $local - $code
		curl -sSfL --globoff -o $RESDIR/strings-$code.xml "https://translate.wordpress.org/projects/android/dev/release-notes/$code/default/export-translations?filters[status]=current&format=android" || (echo Error downloading $code)
	done
}

# Clean up strings by removing starting / trailing whitespaces, convert \n to new lines, etc.
function cleanup() {
	sed 's/\\n/|/g'  \
	| tr '|' '\n' \
	| sed  's/<[^>]*>//g' \
	| sed -e 's/\\'/'/g' \
	| sed 's/^[[:space:]]*//g' \
	| sed 's/[[:space:]]*$//g'
}

function prepare() {
	comment=$1
	footer=$2
	rm -f $RESDIR/release-notes.md
	for line in $(cat $LANG_FILE) ; do
		code=$(echo $line|cut -d "," -f1|tr -d " ")
		name=$(echo $line|cut -d "," -f3-|tr -d " ")
		echo \# $name >> $RESDIR/release-notes.md
		echo >> $RESDIR/release-notes.md
		grep \"$comment\" $RESDIR/strings-$code.xml | cleanup >> $RESDIR/release-notes.md
		grep \"$footer\" $RESDIR/strings-$code.xml | cleanup >> $RESDIR/release-notes.md
		echo >> $RESDIR/release-notes.md
	done
}

fetch
prepare $1 $2
