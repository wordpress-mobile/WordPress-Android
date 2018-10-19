grep -o '<string name=.*</string>' libs/login/WordPressLoginFlow/src/main/res/values/strings.xml > loginstrings.txt;

declare -a exclusions=('default_web_client_id' 'login_notification_channel_id')

found_missing_string=false
string_array=()
IFS=$'\n'
while read -r s; do
  for item in "${exclusions[@]}"; do
      # Skip lookup if login string is one of the exclusions
      [[ $s == *$item* ]] && continue 2
  done
  s=$(echo $s | sed 's/\\/\\\\/g')
  if ! grep -oqr "$s" WordPress/src/main/res/values/strings.xml; then
    string_array+=( $(echo $s | sed 's/\\\\/\\/g') )
    found_missing_string=true
  fi;
done <loginstrings.txt

rm loginstrings.txt

if [ "$found_missing_string" = true ] ; then
  printf "The following string resources are defined in the login library but are missing in (or differ from) the base app's strings.xml:\n\n"
  printf '%s\n' "${string_array[@]}"
  # Exit with error if any strings were missing
  exit 1
fi
