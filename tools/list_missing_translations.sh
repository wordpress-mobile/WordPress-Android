#!/bin/bash

# This script lists all the keys which are missing a translation in each of the Mag16 locales.
# 
# It does so by extracting the name of the keys (which don't have the `translatable="false"` attribute) for each of the Mag6 locales
# and comparing that list with the keys extracted for the originals (`/values/strings.xml`).
#
# The printed output is designed to be easily copy/pasted into a P2 comment to generate a bullet-point list of locales with sublist of missing keys
#

list_translatable_keys() {
  # Extracts the `name` attribute of every `<string>` tag in the `values-$1/strings.xml` file, except for the ones which have the `translatable='false'` attribute
  xpath -q -e "//string[@translatable != 'false']/@name" WordPress/src/main/res/$1/strings.xml | sort | sed 's/^ *name="\(.*\)"$/\`\1\`/'
}

list_diff() {
  # Runs diff with a custom output format so that it can be used as a bullet-point list of missing entries
  diff --unchanged-line-format="" --new-line-format="" --old-line-format="    - %L" "$@"
}

ORIGINAL_KEYS_FILE="${TMPDIR}translatable-strings-originals-keys.txt"
list_translatable_keys "values" >"${ORIGINAL_KEYS_FILE}"

MAG16='ar de es fr he id it ja ko nl pt-rBR ru sv tr zh-rCN zh-rTW'
for locale in $MAG16; do
  DIFF=$(list_diff "${ORIGINAL_KEYS_FILE}" <(list_translatable_keys "values-${locale}"))
  if [ -n "$DIFF" ]; then
    echo "- \`${locale}\`"
    echo "$DIFF"
  fi
done

rm "${ORIGINAL_KEYS_FILE}"
