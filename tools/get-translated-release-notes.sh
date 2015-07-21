#!/bin/sh

LANG_FILE=tools/release-notes-language-codes.csv
TMPDIR=/tmp/release-notes
OUTFILE=$TMPDIR/release-notes.md

function fetch() {
	for line in $(cat $LANG_FILE) ; do
		code=$(echo $line|cut -d "," -f1|tr -d " ")
		local=$(echo $line|cut -d "," -f2|tr -d " ")
		echo updating $local - $code
		mkdir -p $TMPDIR
		curl -sSfL --globoff -o $TMPDIR/strings-$code.xml "http://translate.wordpress.org/projects/apps/android/release-notes/$code/default/export-translations?filters[status]=current&format=android" || (echo Error downloading $code)
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

function extract_release_notes() {
	comment=$1
	footer=$2
	rm -f $OUTFILE
	for line in $(cat $LANG_FILE) ; do
		code=$(echo $line|cut -d "," -f1|tr -d " ")
		name=$(echo $line|cut -d "," -f3-|tr -d " ")
		echo \# $name >> $OUTFILE
		echo >> $OUTFILE
		grep \"$comment\" $TMPDIR/strings-$code.xml | cleanup >> $OUTFILE
		grep \"$footer\" $TMPDIR/strings-$code.xml | cleanup >> $OUTFILE
		echo >> $OUTFILE
	done
}

if [ x"$2" == x ]; then
	echo Usage: $0 RELEASE_NOTES_ID FOOTER_ID
	echo Example: $0 release_note_35 release_note_footer
	exit 1
fi

fetch
extract_release_notes $1 $2
echo Generated file:
echo $OUTFILE
