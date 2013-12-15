LANGS=$(grep "=>" update-translations.rb|cut -d "'" -f 2)
mkdir po-files
for lang in $LANGS; do
    curl "http://translate.wordpress.org/projects/android/dev/$lang/default/export-translations" > po-files/"$lang".po
done
